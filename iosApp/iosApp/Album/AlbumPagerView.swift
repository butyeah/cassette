import Shared
import SwiftUI

/// Swipes sideways through every album of the day, opening on the one that was tapped. Mirrors
/// Android's AlbumPagerScreen.
struct AlbumPagerView: View {
    let sdk: CassetteSdk
    let albumIds: [String]

    @State private var selection: String

    init(sdk: CassetteSdk, albumIds: [String], initialAlbumId: String) {
        self.sdk = sdk
        self.albumIds = albumIds
        _selection = State(initialValue: initialAlbumId)
    }

    var body: some View {
        TabView(selection: $selection) {
            ForEach(albumIds, id: \.self) { id in
                AlbumDetailView(sdk: sdk, albumId: id)
                    .tag(id)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .navigationBarTitleDisplayMode(.inline)
    }
}
