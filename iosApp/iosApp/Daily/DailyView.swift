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
    @State private var isPickingDay = false
    @State private var path = NavigationPath()
    @Environment(DailyReminder.self) private var reminder

    init(sdk: CassetteSdk) {
        self.sdk = sdk
        _model = State(initialValue: DailyViewModel(sdk: sdk))
    }

    var body: some View {
        NavigationStack(path: $path) {
            content
                .background { AnimatedGradientBackground() }
                .overlay(alignment: .bottomLeading) {
                    NowPlayingButton().padding(20)
                }
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        Button { isPickingDay = true } label: {
                            Text(model.day.formatted).font(.pixel(24, weight: .semibold))
                        }
                        .foregroundStyle(.primary)
                        .accessibilityHint("Choose another day")
                    }
                    ToolbarItem(placement: .topBarLeading) {
                        Button { isPickingDay = true } label: {
                            Image(systemName: "calendar")
                        }
                        .accessibilityLabel("Choose another day")
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button { model.toggleLayout() } label: {
                            Image(systemName: model.layout == .grid ? "list.bullet" : "square.grid.2x2")
                        }
                        .accessibilityLabel(model.layout == .grid ? Text("Show as list") : Text("Show as grid"))
                    }
                }
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
                            YearGrid(group: group, expandedAlbumId: model.expandedAlbumId) { albumId in
                                if albumId == model.expandedAlbumId {
                                    path.append(AlbumRoute(albumIds: model.albumIds, albumId: albumId))
                                } else {
                                    withAnimation(.spring(duration: 0.5, bounce: 0.2)) { model.expandedAlbumId = albumId }
                                }
                            }
                        case .list: YearCard(group: group, albumIds: model.albumIds)
                        }
                    }
                }
                .padding(16)
            }
        }
    }
}

/// One year's covers, four tiles to a row, packed by the shared `mosaicCells`: the expanded cover
/// takes 2×2 tiles and the rest flow around it. Tapping a cover calls `onCoverTap`; the caller
/// decides whether that expands it or opens it.
private struct YearGrid: View {
    let group: AlbumsByYear
    let expandedAlbumId: String?
    let onCoverTap: (String) -> Void

    var body: some View {
        let expandedIndex = group.albums.firstIndex { $0.id == expandedAlbumId }
        VStack(alignment: .leading, spacing: 8) {
            Text(String(group.year)).font(.pixel(20, weight: .medium))
            MosaicLayout(cells: mosaicCells(count: group.albums.count, expandedIndex: expandedIndex)) {
                ForEach(group.albums, id: \.id) { album in
                    CoverTile(album: album, expanded: album.id == expandedAlbumId) {
                        onCoverTap(album.id)
                    }
                }
            }
        }
    }
}

/// A cover; while `expanded`, its title and artist fade in over a scrim along the bottom.
private struct CoverTile: View {
    let album: Album
    let expanded: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            CoverImage(url: album.coverURL)
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
