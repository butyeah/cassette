import Foundation
import Observation
import Shared

/// State and actions for the Daily screen: the albums released on one day, across every year.
/// Mirrors Android's OneDayLikeTodayViewModel.
@MainActor
@Observable
final class DailyViewModel {

    enum Layout {
        /// Covers only, four per row, split by release year. The default.
        case grid
        /// Grouped by release year, with title and artist.
        case list
    }

    private(set) var day = MonthDay.today()
    private(set) var isLoading = false
    private(set) var failed = false
    /// Newest year first.
    private(set) var albumsByYear: [AlbumsByYear] = []
    /// Kept for the session only; changing day doesn't reset it.
    var layout = Layout.grid
    /// The grid cover grown to 2×2 with its title, which a second tap opens. Reset on a day
    /// change, as Android's is.
    var expandedAlbumId: String?

    private let sdk: CassetteSdk
    private var hasLoaded = false

    init(sdk: CassetteSdk) {
        self.sdk = sdk
    }

    /// Every album of the day in display order: what the album screen swipes through.
    var albumIds: [String] {
        albumsByYear.flatMap { $0.albums.map(\.id) }
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await load()
    }

    func retry() async {
        await load()
    }

    func select(_ newDay: MonthDay) async {
        guard newDay != day else { return }
        day = newDay
        expandedAlbumId = nil
        await load()
    }

    func toggleLayout() {
        layout = layout == .grid ? .list : .grid
    }

    private func load() async {
        let requested = day
        isLoading = true
        failed = false
        let result: [AlbumsByYear]?
        do {
            result = try await sdk.albumsByDay(month: Int32(requested.month), day: Int32(requested.day))
        } catch {
            result = nil
        }
        // A slower answer for a day the user has already moved on from is dropped.
        guard requested == day else { return }
        albumsByYear = result ?? []
        failed = result == nil
        isLoading = false
    }
}
