import Shared
import SwiftUI

/// The two tabs, as on Android: Daily and Profile.
struct RootView: View {
    let sdk: CassetteSdk

    var body: some View {
        TabView {
            DailyView(sdk: sdk)
                .tabItem { Label("Daily", systemImage: "house") }
            ProfileView()
                .tabItem { Label("Profile", systemImage: "person") }
        }
    }
}
