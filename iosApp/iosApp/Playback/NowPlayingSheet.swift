import Shared
import SwiftUI

/// The round cover of what's playing, floating over the Daily screen; opens `NowPlayingSheet`.
/// Shown once something has played, and kept after it stops so it can be replayed, as on Android.
struct NowPlayingButton: View {
    @Environment(PreviewPlayer.self) private var player
    @State private var isShowingSheet = false

    var body: some View {
        if let nowPlaying = player.nowPlaying {
            Button { isShowingSheet = true } label: {
                CoverImage(url: nowPlaying.album.coverURL, cornerRadius: 32)
                    .frame(width: 64)
                    .overlay {
                        if player.status == .loading {
                            ProgressView().tint(.white)
                        }
                    }
                    .shadow(radius: 6, y: 2)
            }
            .accessibilityLabel("Now playing: \(nowPlaying.trackTitle ?? nowPlaying.album.title)")
            .sheet(isPresented: $isShowingSheet) {
                NowPlayingSheet()
                    .presentationDetents([.medium])
                    .presentationDragIndicator(.visible)
            }
        }
    }
}

/// Cover, track, album and artist, with previous / stop-or-replay / next. Mirrors Android's
/// NowPlayingDialog.
struct NowPlayingSheet: View {
    @Environment(PreviewPlayer.self) private var player

    var body: some View {
        if let nowPlaying = player.nowPlaying {
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
                    .accessibilityLabel(player.status == .stopped ? "Play" : "Stop")

                    Button { player.skipToNext() } label: {
                        Image(systemName: "forward.end.fill").font(.title2)
                    }
                    .disabled(!nowPlaying.hasNext)
                    .accessibilityLabel("Next track")
                }
            }
            .padding(24)
        }
    }
}
