import Shared
import SwiftUI

/// The two tabs, as on Android: Daily and Profile.
struct RootView: View {
    let sdk: CassetteSdk

    private enum Tab { case daily, profile }

    @State private var tab = Tab.daily
    @Environment(DailyReminder.self) private var reminder

    var body: some View {
        TabView(selection: $tab) {
            DailyView(sdk: sdk)
                .tabItem { Label("Daily", systemImage: "house") }
                .tag(Tab.daily)
            ProfileView()
                .tabItem { Label("Profile", systemImage: "person") }
                .tag(Tab.profile)
        }
        // Tapping the daily reminder opens today's albums (DailyView handles the day itself).
        .onChange(of: reminder.openTodayRequests) { tab = .daily }
    }
}
