import AVFoundation
import MediaPlayer
import Observation
import Shared
import UIKit

/// Plays iTunes preview clips, one at a time, app-wide, with autoplay. Mirrors Android's
/// PreviewQueue on top of its PreviewPlayer:
///
/// - Screens start a track with `play`. When a clip plays to its end, the player starts the album's
///   next track that has a preview, then the first preview of each following album of the day,
///   loading those albums itself, so playback carries on after the album screen is gone. It stops
///   after the day's last album.
/// - `nowPlaying` survives `stop` so the track can be `replay`ed; it only changes when another
///   track starts.
///
/// Keeps playing in the background, and shows on the lock screen and in Control Center.
@MainActor
@Observable
final class PreviewPlayer {

    enum Status {
        case stopped
        /// Buffering the clip, or autoplay looking up the next album.
        case loading
        case playing
    }

    private(set) var nowPlaying: NowPlaying?
    /// Seconds left in the clip, or `nil` until its duration is known.
    private(set) var remaining: TimeInterval?

    var status: Status { isAdvancing ? .loading : clipStatus }

    private var clipStatus = Status.stopped
    private var isAdvancing = false

    @ObservationIgnored private let sdk: CassetteSdk
    @ObservationIgnored private let player = AVPlayer()
    @ObservationIgnored private var advanceTask: Task<Void, Never>?
    // Set by stop(): a completion already on its way (the clip ended just as it was stopped) must
    // not carry autoplay on. Cleared whenever a track starts.
    @ObservationIgnored private var isStopped = true
    @ObservationIgnored private var observations: [NSKeyValueObservation] = []
    @ObservationIgnored private var artwork: (albumId: String, image: MPMediaItemArtwork)?

    init(sdk: CassetteSdk) {
        self.sdk = sdk
        observePlayer()
        setUpRemoteCommands()
    }

    /// Whether `position` of album `albumId` is the track that's buffering or playing.
    func isActive(albumId: String, position: Int) -> Bool {
        status != .stopped && nowPlaying?.album.id == albumId && nowPlaying?.position == position
    }

    /// Plays `album`'s preview at `position`, if it has one, and continues through `dayAlbumIds`.
    func play(album: AlbumDetail, previews: [Int: String], position: Int, dayAlbumIds: [String]) {
        cancelAdvance()
        start(NowPlaying(album: album, previews: previews, position: position, dayAlbumIds: dayAlbumIds))
    }

    /// Stops playback and any lookup of the next album; `nowPlaying` stays for `replay`.
    func stop() {
        isStopped = true
        cancelAdvance()
        player.replaceCurrentItem(with: nil)
        clipStatus = .stopped
        remaining = nil
        updateNowPlayingInfo()
    }

    /// Plays `nowPlaying`'s track again from the start, autoplay included.
    func replay() {
        guard let current = nowPlaying else { return }
        cancelAdvance()
        start(current)
    }

    /// The next previewable track: the rest of this album, then the first preview of the day's next
    /// album that has any. With nothing left, playback stops.
    func skipToNext() {
        guard let current = nowPlaying else { return }
        cancelAdvance()
        if let next = current.previews.keys.filter({ $0 > current.position }).min() {
            start(current.at(position: next))
        } else {
            advanceTask = Task { await playAdjacentAlbum(from: current, forward: true) }
        }
    }

    /// The previous previewable track: earlier in this album, then the **last** preview of the
    /// day's previous album that has any. With nothing earlier, the current track carries on.
    func skipToPrevious() {
        guard let current = nowPlaying else { return }
        cancelAdvance()
        if let previous = current.previews.keys.filter({ $0 < current.position }).max() {
            start(current.at(position: previous))
        } else {
            advanceTask = Task { await playAdjacentAlbum(from: current, forward: false) }
        }
    }

    // MARK: - Playback

    private func start(_ next: NowPlaying) {
        guard let urlString = next.url, let url = URL(string: urlString) else { return }
        isStopped = false
        nowPlaying = next
        remaining = nil
        clipStatus = .loading
        activateAudioSession()
        player.replaceCurrentItem(with: AVPlayerItem(url: url))
        player.play()
        updateNowPlayingInfo()
    }

    private func cancelAdvance() {
        advanceTask?.cancel()
        advanceTask = nil
        isAdvancing = false
    }

    private func onClipEnded() {
        guard !isStopped else { return }
        clipStatus = .stopped
        skipToNext()
    }

    /// Plays the nearest album after (or, if not `forward`, before) `current`'s that loads and has
    /// previews: from its first preview going forward, its last going back. Albums that fail to
    /// load or have nothing to preview are skipped. The current clip keeps playing meanwhile.
    private func playAdjacentAlbum(from current: NowPlaying, forward: Bool) async {
        guard let index = current.dayAlbumIds.firstIndex(of: current.album.id) else { return }
        let candidates = forward
            ? Array(current.dayAlbumIds[(index + 1)...])
            : Array(current.dayAlbumIds[..<index].reversed())
        isAdvancing = true
        // A cancelled lookup leaves the flag alone: cancelAdvance() already cleared it, and a newer
        // lookup may have set it again since.
        defer { if !Task.isCancelled { isAdvancing = false } }

        // The Kotlin calls don't observe Swift task cancellation, so each result is checked after
        // it arrives: a skip or stop in the meantime wins.
        for albumId in candidates {
            let album = try? await sdk.albumDetail(id: albumId)
            if Task.isCancelled { return }
            guard let album else { continue }
            let previews = await sdk.previews(for: album)
            if Task.isCancelled { return }
            guard let position = forward ? previews.keys.min() : previews.keys.max() else { continue }
            start(NowPlaying(album: album, previews: previews, position: position, dayAlbumIds: current.dayAlbumIds))
            return
        }
        // Past the day's last album there's nothing more to play; before its first, the current
        // track just carries on.
        if forward {
            stop()
        }
    }

    private func observePlayer() {
        // KVO calls back on whatever thread changed the value. By the time the hop to the main actor
        // runs, another clip may have started, so the handler reads the player's current state
        // rather than the value that triggered it.
        observations.append(player.observe(\.timeControlStatus) { [weak self] _, _ in
            Task { @MainActor in self?.syncClipStatus() }
        })

        player.addPeriodicTimeObserver(forInterval: CMTime(seconds: 0.25, preferredTimescale: 600), queue: .main) { [weak self] time in
            MainActor.assumeIsolated {
                guard let self, let item = self.player.currentItem, item.duration.isNumeric else { return }
                self.remaining = max(item.duration.seconds - time.seconds, 0)
            }
        }

        NotificationCenter.default.addObserver(forName: AVPlayerItem.didPlayToEndTimeNotification, object: nil, queue: .main) { [weak self] notification in
            MainActor.assumeIsolated {
                guard let self, (notification.object as? AVPlayerItem) === self.player.currentItem else { return }
                self.onClipEnded()
            }
        }

        NotificationCenter.default.addObserver(forName: AVPlayerItem.failedToPlayToEndTimeNotification, object: nil, queue: .main) { [weak self] notification in
            MainActor.assumeIsolated {
                // A clip that fails doesn't count as finished, so autoplay doesn't skip past it.
                guard let self, (notification.object as? AVPlayerItem) === self.player.currentItem else { return }
                self.stop()
            }
        }
    }

    private func syncClipStatus() {
        guard !isStopped, player.currentItem != nil else { return }
        switch player.timeControlStatus {
        case .playing: clipStatus = .playing
        case .waitingToPlayAtSpecifiedRate: clipStatus = .loading
        // Paused by the system (a call, another app taking audio). Not a completion, so autoplay
        // doesn't move on; the clip can be replayed from the now-playing controls.
        case .paused: clipStatus = .stopped
        @unknown default: break
        }
        updateNowPlayingInfo()
    }

    private func activateAudioSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default)
        try? session.setActive(true)
    }

    // MARK: - Lock screen and Control Center

    private func setUpRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { self?.replay() }
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { self?.stop() }
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.status == .stopped ? self.replay() : self.stop()
            }
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { self?.skipToNext() }
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { self?.skipToPrevious() }
            return .success
        }
        // Clips are 30 seconds; seeking isn't offered, as on Android.
        center.changePlaybackPositionCommand.isEnabled = false
    }

    private func updateNowPlayingInfo() {
        guard let current = nowPlaying else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }
        let center = MPRemoteCommandCenter.shared()
        center.nextTrackCommand.isEnabled = current.hasNext
        center.previousTrackCommand.isEnabled = current.hasPrevious

        var info: [String: Any] = [
            MPMediaItemPropertyTitle: current.trackTitle ?? current.album.title,
            MPMediaItemPropertyArtist: current.album.artistName,
            MPMediaItemPropertyAlbumTitle: current.album.title,
            MPNowPlayingInfoPropertyPlaybackRate: clipStatus == .playing ? 1.0 : 0.0,
        ]
        if let item = player.currentItem, item.duration.isNumeric {
            info[MPMediaItemPropertyPlaybackDuration] = item.duration.seconds
            info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = player.currentTime().seconds
        }
        if let artwork, artwork.albumId == current.album.id {
            info[MPMediaItemPropertyArtwork] = artwork.image
        } else {
            loadArtwork(for: current.album)
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func loadArtwork(for album: AlbumDetail) {
        guard let url = album.coverURL else { return }
        let albumId = album.id
        Task {
            guard let (data, _) = try? await URLSession.shared.data(from: url),
                  let image = UIImage(data: data) else { return }
            let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
            self.artwork = (albumId, artwork)
            if self.nowPlaying?.album.id == albumId {
                self.updateNowPlayingInfo()
            }
        }
    }
}
