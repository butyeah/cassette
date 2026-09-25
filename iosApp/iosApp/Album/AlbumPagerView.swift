import Shared
import SwiftUI

/// Swipes sideways through every album of the day, opening on the one that was tapped. Mirrors
/// Android's AlbumPagerScreen: paging away from the album that's playing stops it, and when
/// autoplay moves on to another of these albums, the pager follows.
struct AlbumPagerView: View {
    let sdk: CassetteSdk
    let albumIds: [String]

    @State private var selection: String
    @Environment(PreviewPlayer.self) private var player

    init(sdk: CassetteSdk, albumIds: [String], initialAlbumId: String) {
        self.sdk = sdk
        self.albumIds = albumIds
        _selection = State(initialValue: initialAlbumId)
    }

    private var playingAlbumId: String? {
        player.status == .stopped ? nil : player.nowPlaying?.album.id
    }

    var body: some View {
        TabView(selection: $selection) {
            ForEach(albumIds, id: \.self) { id in
                AlbumDetailView(sdk: sdk, albumId: id, dayAlbumIds: albumIds)
                    .tag(id)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: playingAlbumId) { _, albumId in
            if let albumId, albumId != selection, albumIds.contains(albumId) {
                withAnimation { selection = albumId }
            }
        }
        .onChange(of: selection) { _, albumId in
            // Following autoplay lands on the playing album, so only a swipe away stops it.
            if let playingAlbumId, playingAlbumId != albumId {
                player.stop()
            }
        }
    }
}
