import Shared
import SwiftUI

/// Cover, track, album and artist, with previous / stop-or-replay / next, then the track's lyrics.
/// Mirrors Android's NowPlayingDialog. It scrolls, so long lyrics never push the controls away.
struct NowPlayingSheet: View {
    let sdk: CassetteSdk

    @Environment(PreviewPlayer.self) private var player

    var body: some View {
        if let nowPlaying = player.nowPlaying {
            ScrollView {
                content(nowPlaying)
                    .padding(24)
            }
        }
    }

    private func content(_ nowPlaying: NowPlaying) -> some View {
        VStack(spacing: 16) {
            CoverImage(url: nowPlaying.album.coverURL, cornerRadius: 12)
                .frame(maxWidth: 220)
            VStack(spacing: 4) {
                Text(nowPlaying.trackTitle ?? nowPlaying.album.title)
                    .font(.pixel(22, weight: .semibold))
                Text("\(nowPlaying.album.title) · \(nowPlaying.album.artistName)")
                    .font(.handjet(20))
                    .foregroundStyle(.secondary)
            }
            .multilineTextAlignment(.center)
            .lineLimit(2)

            HStack(spacing: 36) {
                Button { player.skipToPrevious() } label: {
                    Image(systemName: "backward.end.fill").font(.title2)
                }
                .disabled(!nowPlaying.hasPrevious)
                .accessibilityLabel("Previous track")

                Button {
                    player.status == .stopped ? player.replay() : player.stop()
                } label: {
                    ZStack {
                        Circle().fill(.tint).frame(width: 68, height: 68)
                        if player.status == .loading {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: player.status == .stopped ? "play.fill" : "stop.fill")
                                .font(.title)
                                .foregroundStyle(.white)
                        }
                    }
                }
                .accessibilityLabel(player.status == .stopped ? Text("Play") : Text("Stop"))

                Button { player.skipToNext() } label: {
                    Image(systemName: "forward.end.fill").font(.title2)
                }
                .disabled(!nowPlaying.hasNext)
                .accessibilityLabel("Next track")
            }

            LyricsSection(sdk: sdk, album: nowPlaying.album, position: nowPlaying.position)
                .padding(.top, 8)
        }
    }
}

/// The track's lyrics under a heading, or why there are none, then LRCLIB's credit. A new track
/// starts a new lookup, cancelling one still running for the last.
private struct LyricsSection: View {
    let sdk: CassetteSdk
    let album: AlbumDetail
    let position: Int

    private enum Phase {
        case loading
        case found(String)
        case instrumental
        case notFound
        case failed
    }

    @State private var phase = Phase.loading

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Lyrics")
                .font(.pixel(20, weight: .medium))
                .accessibilityAddTraits(.isHeader)
            switch phase {
            case .loading:
                ProgressView()
            case .found(let text):
                Text(text)
                    .font(.handjet(20))
                    .textSelection(.enabled)
            case .instrumental:
                message("Instrumental")
            case .notFound:
                message("No lyrics found for this track")
            case .failed:
                message("Couldn't load the lyrics")
            }
            Text("Lyrics from LRCLIB")
                .font(.handjet(16))
                .foregroundStyle(.secondary)
                .padding(.top, 8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .task(id: "\(album.id)#\(position)") { await load() }
    }

    private func message(_ text: LocalizedStringKey) -> some View {
        Text(text).font(.handjet(20)).foregroundStyle(.secondary)
    }

    private func load() async {
        phase = .loading
        let result: Phase
        do {
            switch try await sdk.trackLyrics(album: album, position: Int32(position)) {
            case let plain as LyricsPlain: result = .found(plain.text)
            case is LyricsInstrumental: result = .instrumental
            default: result = .notFound
            }
        } catch {
            result = .failed
        }
        guard !Task.isCancelled else { return }
        phase = result
    }
}
