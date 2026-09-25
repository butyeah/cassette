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
                        .accessibilityLabel(model.layout == .grid ? "Show as list" : "Show as grid")
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
                        case .grid: YearGrid(group: group, albumIds: model.albumIds)
                        case .list: YearCard(group: group, albumIds: model.albumIds)
                        }
                    }
                }
                .padding(16)
            }
        }
    }
}

/// One year's covers, four per row.
private struct YearGrid: View {
    let group: AlbumsByYear
    let albumIds: [String]

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 4)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(group.year)).font(.pixel(20, weight: .medium))
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(group.albums, id: \.id) { album in
                    NavigationLink(value: AlbumRoute(albumIds: albumIds, albumId: album.id)) {
                        CoverImage(url: album.coverURL)
                    }
                    .accessibilityLabel("\(album.title) by \(album.artistName)")
                }
            }
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
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
