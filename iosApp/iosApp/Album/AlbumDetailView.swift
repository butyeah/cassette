import Shared
import SwiftUI

/// One album: cover, details, streaming links and tracklist.
struct AlbumDetailView: View {
    @State private var model: AlbumDetailViewModel

    init(sdk: CassetteSdk, albumId: String) {
        _model = State(initialValue: AlbumDetailViewModel(sdk: sdk, albumId: albumId))
    }

    var body: some View {
        Group {
            if let album = model.album {
                AlbumContent(album: album)
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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(album.title).font(.pixel(30, weight: .semibold))
                    Text(album.artistName).font(.pixel(17, weight: .medium))
                }

                CoverImage(url: album.coverURL, cornerRadius: 12)
                    .accessibilityLabel("\(album.title) by \(album.artistName)")

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
                            TrackRow(track: track)
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

private struct TrackRow: View {
    let track: Track

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text("\(track.position). \(track.title)")
            Spacer()
            if let duration = track.durationText {
                Text(duration).foregroundStyle(.secondary).monospacedDigit()
            }
        }
        .font(.handjet(21))
        .padding(.vertical, 10)
    }
}
