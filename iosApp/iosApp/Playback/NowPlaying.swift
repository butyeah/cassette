import Foundation
import Shared

/// The track the preview player last started, with what it needs to carry on from there: the
/// album's `previews` (URL by track position) and the `dayAlbumIds` autoplay continues through.
/// Mirrors Android's NowPlaying.
struct NowPlaying {
    let album: AlbumDetail
    let previews: [Int: String]
    let position: Int
    let dayAlbumIds: [String]

    var url: String? { previews[position] }

    var trackTitle: String? {
        album.tracks.first { Int($0.position) == position }?.title
    }

    private var albumIndex: Int? { dayAlbumIds.firstIndex(of: album.id) }

    /// Whether ⏭ can go anywhere: a later preview here, or a later album of the day.
    var hasNext: Bool {
        previews.keys.contains { $0 > position } || albumIndex.map { $0 < dayAlbumIds.count - 1 } == true
    }

    /// Whether ⏮ can go anywhere: an earlier preview here, or an earlier album of the day.
    var hasPrevious: Bool {
        previews.keys.contains { $0 < position } || albumIndex.map { $0 > 0 } == true
    }

    func at(position: Int) -> NowPlaying {
        NowPlaying(album: album, previews: previews, position: position, dayAlbumIds: dayAlbumIds)
    }
}

extension CassetteSdk {
    /// Preview URL by track position, or none when iTunes has nothing or the lookup fails: a
    /// missing preview just means no play button, never an error.
    ///
    /// `@MainActor` like every other Kotlin call site: a nonisolated async function runs on a
    /// background thread, and Kotlin/Native used to crash when a suspend function started there.
    @MainActor
    func previews(for album: AlbumDetail) async -> [Int: String] {
        guard let previews = try? await trackPreviews(album: album) else { return [:] }
        return Dictionary(uniqueKeysWithValues: previews.map { (Int($0.key.int32Value), $0.value) })
    }
}

/// The countdown next to a playing track, `mm:ss`. Rounds up, so a clip starts on its full length
/// (`00:30`) and only reads `00:00` once it's over; `--:--` until the clip's duration is known.
/// Same as Android's remainingTimeText.
func remainingTimeText(_ remaining: TimeInterval?) -> String {
    guard let remaining else { return "--:--" }
    let totalSeconds = Int(max(remaining, 0).rounded(.up))
    return String(format: "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}
