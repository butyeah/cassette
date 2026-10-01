import Shared
import SwiftUI

/// Which album to open, and the day's albums to swipe through from there.
struct AlbumRoute: Hashable {
    let albumIds: [String]
    let albumId: String
}

/// The albums released on one day across every year, as a cover grid or a list, grouped by year.
struct DailyView: View {
    let sdk: CassetteSdk

    @State private var model: DailyViewModel
    @Binding var path: NavigationPath

    @State private var isPickingDay = false
    /// The expanded grid cover's dominant colours, for the background. Kept while moving from one
    /// expanded cover to another, so the background fades straight between their colours.
    @State private var expandedCoverColors: [Color]?
    @Environment(DailyReminder.self) private var reminder

    init(sdk: CassetteSdk, path: Binding<NavigationPath>) {
        self.sdk = sdk
        _path = path
        _model = State(initialValue: DailyViewModel(sdk: sdk))
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                DayHeader(
                    day: model.day,
                    layout: model.layout,
                    onDayTap: { isPickingDay = true },
                    onToggleLayout: { model.toggleLayout() }
                )
                content
            }
            .background {
                ZStack {
                    Color(light: 0xFFFFFF, dark: 0x000000).ignoresSafeArea()
                    AnimatedGradientBackground(colors: model.layout == .grid ? expandedCoverColors : nil)
                }
            }
            .onChange(of: model.expandedAlbumId) {
                if model.expandedAlbumId == nil { expandedCoverColors = nil }
            }
            // The header above stands in for the navigation bar, as on Android, so it isn't
            // turned into toolbar glass. Pushed album screens keep their own bar.
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: AlbumRoute.self) { route in
                AlbumPagerView(sdk: sdk, albumIds: route.albumIds, initialAlbumId: route.albumId)
            }
            .sheet(isPresented: $isPickingDay) {
                DayPickerSheet(day: model.day) { day in
                    Task { await model.select(day) }
                }
            }
            .task { await model.loadIfNeeded() }
            .onChange(of: reminder.openTodayRequests) {
                path = NavigationPath()
                Task { await model.select(.today()) }
            }
        }
    }

    /// Takes the background's colours from `cover`, the expanded album's. Dropped if another cover
    /// has been expanded, or none, by the time they're ready.
    private func takeColors(from cover: UIImage, albumId: String) {
        guard let cgImage = cover.cgImage else { return }
        Task {
            let colors = await dominantColors(in: cgImage)
            guard model.expandedAlbumId == albumId, !colors.isEmpty else { return }
            expandedCoverColors = colors.map(\.color)
        }
    }

    @ViewBuilder
    private var content: some View {
        if model.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if model.failed {
            VStack(spacing: 16) {
                Text("Couldn't load the albums.").font(.handjet(22))
                Button("Retry") { Task { await model.retry() } }
                    .font(.pixel(17))
                    .buttonStyle(.bordered)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if model.albumsByYear.isEmpty {
            Text("No albums released on this day")
                .font(.handjet(22))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    ForEach(model.albumsByYear, id: \.year) { group in
                        switch model.layout {
                        case .grid:
                            YearGrid(
                                group: group,
                                expandedAlbumId: model.expandedAlbumId,
                                onCoverTap: { albumId in
                                    if albumId == model.expandedAlbumId {
                                        path.append(AlbumRoute(albumIds: model.albumIds, albumId: albumId))
                                    } else {
                                        withAnimation(.spring(duration: 0.5, bounce: 0.2)) { model.expandedAlbumId = albumId }
                                    }
                                },
                                onExpandedCoverLoaded: takeColors
                            )
                        case .list: YearCard(group: group, albumIds: model.albumIds)
                        }
                    }
                }
                .padding(16)
            }
        }
    }
}

/// The day as an outlined button that opens the day picker, with the list/grid toggle to its right.
private struct DayHeader: View {
    let day: MonthDay
    let layout: DailyViewModel.Layout
    let onDayTap: () -> Void
    let onToggleLayout: () -> Void

    /// The toggle's width, also left empty on the other side so the day stays centred.
    private static let toggleWidth: CGFloat = 44

    var body: some View {
        HStack(spacing: 8) {
            Color.clear.frame(width: Self.toggleWidth, height: 1)
            Button(action: onDayTap) {
                HStack(spacing: 8) {
                    Image(systemName: "calendar")
                    Text(day.formattedUppercase)
                        .font(.pixel(22, weight: .bold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Image(systemName: "chevron.down").font(.body.weight(.semibold))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(.primary, lineWidth: 2))
                .contentShape(RoundedRectangle(cornerRadius: 8))
            }
            .buttonStyle(.plain)
            .accessibilityHint("Choose another day")
            Button(action: onToggleLayout) {
                Image(systemName: layout == .grid ? "list.bullet" : "square.grid.2x2")
                    .font(.title3)
                    .frame(width: Self.toggleWidth, height: Self.toggleWidth)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(layout == .grid ? Text("Show as list") : Text("Show as grid"))
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
    }
}

/// One year's covers, four tiles to a row, packed by the shared `mosaicCells`: the expanded cover
/// takes 2×2 tiles and the rest flow around it. Tapping a cover calls `onCoverTap`; the caller
/// decides whether that expands it or opens it. `onExpandedCoverLoaded` gets the expanded cover's
/// image and album ID once it has loaded.
private struct YearGrid: View {
    let group: AlbumsByYear
    let expandedAlbumId: String?
    let onCoverTap: (String) -> Void
    let onExpandedCoverLoaded: (UIImage, String) -> Void

    var body: some View {
        let expandedIndex = group.albums.firstIndex { $0.id == expandedAlbumId }
        VStack(alignment: .leading, spacing: 8) {
            Text(String(group.year)).font(.pixel(20, weight: .medium))
            MosaicLayout(cells: mosaicCells(count: group.albums.count, expandedIndex: expandedIndex)) {
                ForEach(group.albums, id: \.id) { album in
                    CoverTile(
                        album: album,
                        expanded: album.id == expandedAlbumId,
                        onTap: { onCoverTap(album.id) },
                        onExpandedCoverLoaded: { onExpandedCoverLoaded($0, album.id) }
                    )
                }
            }
        }
    }
}

/// A cover; while `expanded`, its title and artist fade in over a scrim along the bottom, and its
/// loaded image goes to `onExpandedCoverLoaded`.
private struct CoverTile: View {
    let album: Album
    let expanded: Bool
    let onTap: () -> Void
    let onExpandedCoverLoaded: (UIImage) -> Void

    /// Kept rather than reported straight from `onLoad`: a cover usually loads long before it's
    /// expanded, and expanding it doesn't load it again.
    @State private var cover: UIImage?

    var body: some View {
        Button(action: onTap) {
            CoverImage(url: album.coverURL) { cover = $0 }
                .overlay(alignment: .bottomLeading) {
                    if expanded {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(album.title).font(.pixel(15, weight: .medium)).lineLimit(2)
                            Text(album.artistName).font(.handjet(17)).lineLimit(1)
                        }
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.leading)
                        .padding(8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(LinearGradient(colors: [.clear, .black.opacity(0.7)], startPoint: .top, endPoint: .bottom))
                        .clipShape(UnevenRoundedRectangle(bottomLeadingRadius: 8, bottomTrailingRadius: 8))
                        .transition(.opacity)
                    }
                }
        }
        .buttonStyle(.plain)
        // The cover's label already names the album and artist.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(album.title) by \(album.artistName)")
        .accessibilityHint(expanded ? Text("Open album") : Text("Show details"))
        .accessibilityAddTraits(.isButton)
        .onChange(of: expanded && cover != nil, initial: true) {
            if expanded, let cover { onExpandedCoverLoaded(cover) }
        }
    }
}

/// One year's albums as rows with title and artist, on a card.
private struct YearCard: View {
    let group: AlbumsByYear
    let albumIds: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(group.year)).font(.pixel(20, weight: .medium))
            ForEach(group.albums, id: \.id) { album in
                NavigationLink(value: AlbumRoute(albumIds: albumIds, albumId: album.id)) {
                    HStack(spacing: 14) {
                        CoverImage(url: album.coverURL, cornerRadius: 4)
                            .frame(width: 56)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(album.title).font(.handjet(22))
                            Text(album.artistName).font(.handjet(18)).foregroundStyle(.secondary)
                        }
                        .multilineTextAlignment(.leading)
                        Spacer(minLength: 0)
                    }
                }
                .foregroundStyle(.primary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        // Frosted glass over the gradient, as Android's CoverCard is.
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
