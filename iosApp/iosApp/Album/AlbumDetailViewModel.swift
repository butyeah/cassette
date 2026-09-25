import Foundation
import Observation
import Shared

/// State for one album page: its details from the offline index, or live MusicBrainz when it isn't
/// there (see GetAlbumDetailUseCase). Mirrors Android's AlbumDetailViewModel, minus previews,
/// which come with playback.
@MainActor
@Observable
final class AlbumDetailViewModel {
    private(set) var album: AlbumDetail?
    private(set) var isLoading = false
    private(set) var failed = false

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
    }
}
