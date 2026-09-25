import Foundation
import Observation
import Shared

/// State for one album page: its details from the offline index, or live MusicBrainz when it isn't
/// there (see GetAlbumDetailUseCase), then its previews. Mirrors Android's AlbumDetailViewModel.
@MainActor
@Observable
final class AlbumDetailViewModel {
    private(set) var album: AlbumDetail?
    private(set) var isLoading = false
    private(set) var failed = false
    /// Preview URL by track position. Loaded after the album and never blocks it: stays empty when
    /// iTunes has nothing, and tracks missing from it just get no play button.
    private(set) var previews: [Int: String] = [:]

    private let sdk: CassetteSdk
    private let albumId: String

    init(sdk: CassetteSdk, albumId: String) {
        self.sdk = sdk
        self.albumId = albumId
    }

    /// Loads once per page; paging back to an album doesn't look it up again.
    func loadIfNeeded() async {
        guard album == nil, !isLoading else { return }
        await load()
    }

    func retry() async {
        await load()
    }

    private func load() async {
        isLoading = true
        failed = false
        do {
            album = try await sdk.albumDetail(id: albumId)
        } catch {
            failed = true
        }
        isLoading = false
        if let album {
            previews = await sdk.previews(for: album)
        }
    }
}
