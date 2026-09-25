import Shared
import SwiftUI

/// One album: cover, details, streaming links and tracklist, with a preview per track where iTunes
/// has one. `dayAlbumIds` is what autoplay carries on through after this album.
struct AlbumDetailView: View {
    let dayAlbumIds: [String]

    @State private var model: AlbumDetailViewModel

    init(sdk: CassetteSdk, albumId: String, dayAlbumIds: [String]) {
        self.dayAlbumIds = dayAlbumIds
        _model = State(initialValue: AlbumDetailViewModel(sdk: sdk, albumId: albumId))
    }

    var body: some View {
        Group {
            if let album = model.album {
                AlbumContent(album: album, previews: model.previews, dayAlbumIds: dayAlbumIds)
            } else if model.failed {
                VStack(spacing: 16) {
                    Text("Couldn't load this album.").font(.handjet(22))
                    Button("Retry") { Task { await model.retry() } }
                        .font(.pixel(17))
                        .buttonStyle(.bordered)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task { await model.loadIfNeeded() }
    }
}

private struct AlbumContent: View {
    let album: AlbumDetail
    let previews: [Int: String]
    let dayAlbumIds: [String]

    @Environment(PreviewPlayer.self) private var player

    /// This album's track that's buffering or playing, if any.
    private var activePosition: Int? {
        guard player.status != .stopped, let nowPlaying = player.nowPlaying,
              nowPlaying.album.id == album.id else { return nil }
        return nowPlaying.position
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(album.title).font(.pixel(30, weight: .semibold))
                    Text(album.artistName).font(.pixel(17, weight: .medium))
                }

                CoverImage(url: album.coverURL, cornerRadius: 12)
                    .accessibilityLabel("\(album.title) by \(album.artistName)")

                if !previews.isEmpty {
                    PreviewDisplay(album: album, activePosition: activePosition)
                }

                VStack(alignment: .leading, spacing: 4) {
                    ForEach([album.typeAndYear, album.genresText, album.ratingText].compactMap { $0 }, id: \.self) {
                        Text($0).font(.handjet(20)).foregroundStyle(.secondary)
                    }
                }

                let links = album.streamingLinks.displayList
                if !links.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(links, id: \.label) { link in
                                Link(link.label, destination: link.url)
                                    .font(.pixel(15))
                                    .buttonStyle(.bordered)
                                    .buttonBorderShape(.capsule)
                            }
                        }
                    }
                }

                if !album.tracks.isEmpty {
                    Text("Tracklist").font(.pixel(20, weight: .medium))
                        .padding(.top, 8)
                    VStack(spacing: 0) {
                        ForEach(album.tracks, id: \.position) { track in
                            let position = Int(track.position)
                            TrackRow(
                                track: track,
                                hasPreview: previews[position] != nil,
                                isActive: activePosition == position
                            ) {
                                if activePosition == position {
                                    player.stop()
                                } else {
                                    player.play(album: album, previews: previews, position: position, dayAlbumIds: dayAlbumIds)
                                }
                            }
                            if track != album.tracks.last {
                                Divider()
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }
}

/// The track playing and its countdown, under the cover. Mirrors Android's preview display.
private struct PreviewDisplay: View {
    let album: AlbumDetail
    let activePosition: Int?

    @Environment(PreviewPlayer.self) private var player

    var body: some View {
        let title = activePosition.flatMap { position in album.tracks.first { Int($0.position) == position }?.title }
        HStack(spacing: 10) {
            Image(systemName: activePosition == nil ? "play.fill" : "waveform")
                .symbolEffect(.variableColor.iterative, isActive: activePosition != nil && player.status == .playing)
            Text(activePosition.map { "Track \($0)" } ?? "Track --")
                .font(.pixel(15, weight: .medium))
            Text(title ?? "--")
                .font(.handjet(19))
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(remainingTimeText(activePosition == nil ? nil : player.remaining))
                .font(.pixel(15))
                .monospacedDigit()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .foregroundStyle(.white)
        .background(.black, in: RoundedRectangle(cornerRadius: 10))
        .accessibilityElement(children: .combine)
    }
}

private struct TrackRow: View {
    let track: Track
    let hasPreview: Bool
    let isActive: Bool
    let onTogglePreview: () -> Void

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text("\(track.position). \(track.title)")
            Spacer()
            if let duration = track.durationText {
                Text(duration).foregroundStyle(isActive ? .primary : .secondary).monospacedDigit()
            }
            if hasPreview {
                Button(action: onTogglePreview) {
                    Image(systemName: isActive ? "stop.fill" : "play.fill")
                        .frame(width: 28)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(isActive ? "Stop preview of \(track.title)" : "Play preview of \(track.title)")
            }
        }
        .font(.handjet(21))
        .padding(.vertical, 10)
        .padding(.horizontal, isActive ? 12 : 0)
        .foregroundStyle(isActive ? Color(.systemBackground) : Color.primary)
        .background {
            if isActive {
                Capsule().fill(Color.primary)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { if hasPreview { onTogglePreview() } }
        .animation(.easeInOut(duration: 0.2), value: isActive)
    }
}
