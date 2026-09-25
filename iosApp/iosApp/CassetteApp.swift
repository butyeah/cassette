import Shared
import SwiftUI

@main
struct CassetteApp: App {
    /// One for the app's lifetime: the MusicBrainz rate limit only holds while every call shares it.
    private let sdk: CassetteSdk
    /// App-wide, so one clip plays at a time and autoplay outlives the album screen.
    @State private var player: PreviewPlayer

    init() {
        // Covers are loaded with AsyncImage, which only caches through URLCache; the default one is
        // too small to keep a day's worth of them.
        URLCache.shared = URLCache(memoryCapacity: 50_000_000, diskCapacity: 200_000_000)

        let sdk = CassetteSdk(
            appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0",
            sessionStore: KeychainSessionStore()
        )
        self.sdk = sdk
        _player = State(initialValue: PreviewPlayer(sdk: sdk))
    }

    var body: some Scene {
        WindowGroup {
            DailyView(sdk: sdk)
                .environment(player)
        }
    }
}
