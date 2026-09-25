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
    /// The cover's dominant colours once it has loaded; the background uses the palette until then.
    @State private var dominant: [RGB]?

    private static let coverSize: CGFloat = 200

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

                // A player-style display panel beside the cover, as on Android.
                HStack(spacing: 12) {
                    PreviewDisplay(album: album, activePosition: activePosition, dominant: dominant)
                    CoverImage(url: album.coverURL) { image in
                        guard dominant == nil, let cgImage = image.cgImage else { return }
                        Task { dominant = await dominantColors(in: cgImage) }
                    }
                    .frame(width: Self.coverSize)
                    .accessibilityLabel("\(album.title) by \(album.artistName)")
                }
                .frame(height: Self.coverSize)
                .background(.black, in: RoundedRectangle(cornerRadius: 8))

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
                    let highlight = highlightContainerColors(dominant: dominant)
                    VStack(spacing: 4) {
                        ForEach(album.tracks, id: \.position) { track in
                            let position = Int(track.position)
                            TrackRow(
                                track: track,
                                hasPreview: previews[position] != nil,
                                isActive: activePosition == position,
                                isPlaying: activePosition == position && player.status == .playing,
                                highlight: highlight
                            ) {
                                if activePosition == position {
                                    player.stop()
                                } else {
                                    player.play(album: album, previews: previews, position: position, dayAlbumIds: dayAlbumIds)
                                }
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
        .background { AnimatedGradientBackground(colors: dominant?.map(\.color)) }
    }
}

/// The black display panel beside the cover: a readout of the track playing and its countdown
/// across the top, in the cover's colours, and the waveform along the bottom. Mirrors Android's
/// preview display.
private struct PreviewDisplay: View {
    let album: AlbumDetail
    let activePosition: Int?
    let dominant: [RGB]?

    @Environment(PreviewPlayer.self) private var player
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let title = activePosition.flatMap { position in album.tracks.first { Int($0.position) == position }?.title }
        let textColor = displayTextColor(dominant: dominant, background: .black).color
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 4) {
                // Media-control style: ▶ while stopped, ❚❚ while a clip is buffering or playing.
                Image(systemName: activePosition == nil ? "play.fill" : "pause.fill")
                    .font(.system(size: 13))
                    .contentTransition(.symbolEffect(.replace))
                    .accessibilityHidden(true)
                Text(activePosition.map { String(localized: "Track \($0)") } ?? String(localized: "Track --"))
                    .font(.pixel(15, weight: .medium))
                    .lineLimit(1)
                Spacer(minLength: 4)
                Text(remainingTimeText(activePosition == nil ? nil : player.remaining))
                    .font(.pixel(15))
                    .monospacedDigit()
            }
            .foregroundStyle(textColor)

            // The text colour is readable on black, so black is readable on it.
            Text(title ?? "--")
                .font(.handjet(19))
                .lineLimit(1)
                .foregroundStyle(.black)
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(textColor, in: RoundedRectangle(cornerRadius: 4))

            Spacer(minLength: 0)

            PreviewWaveform(
                playing: activePosition != nil && player.status == .playing,
                colors: waveformColors(
                    dominant: dominant,
                    fallback: Palette.rgb(for: colorScheme),
                    background: .black,
                    count: previewWaveformLines
                ).map(\.color)
            )
        }
        .padding(12)
        .accessibilityElement(children: .combine)
    }
}

private struct TrackRow: View {
    let track: Track
    let hasPreview: Bool
    /// Buffering or playing.
    let isActive: Bool
    /// Audible: the highlight's edges ripple.
    let isPlaying: Bool
    /// The highlight's colours from the cover; `nil` falls back to the primary colour.
    let highlight: ContainerColors?
    let onTogglePreview: () -> Void

    var body: some View {
        let container = highlight?.container.color ?? Color.primary
        let content = highlight?.content.color ?? Color(.systemBackground)
        HStack(alignment: .firstTextBaseline) {
            Text("\(track.position). \(track.title)")
                .lineLimit(1)
            Spacer()
            if let duration = track.durationText {
                Text(duration).foregroundStyle(isActive ? content : .secondary).monospacedDigit()
            }
            if hasPreview {
                Button(action: onTogglePreview) {
                    Image(systemName: isActive ? "stop.fill" : "play.fill")
                        .frame(width: 28)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(isActive ? Text("Stop preview of \(track.title)") : Text("Play preview of \(track.title)"))
            }
        }
        .font(.handjet(21))
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .foregroundStyle(isActive ? content : Color.primary)
        .background {
            if isActive {
                // M3 Expressive's active indicator: its edges ripple like the waveform while the
                // clip is audible, and ease flat while it buffers.
                WaveAnimation(running: isPlaying) { amplitude, time in
                    WavyPill(
                        amplitude: amplitude,
                        phase: wavePhase(time: time, speed: wavyPillWaveSpeed, wavelength: wavyPillWavelength)
                    )
                    .fill(container)
                }
                .transition(.opacity)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { if hasPreview { onTogglePreview() } }
        .animation(.easeInOut(duration: 0.2), value: isActive)
        .animation(.easeInOut(duration: 0.3), value: highlight)
    }
}
