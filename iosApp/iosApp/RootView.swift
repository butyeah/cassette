import Shared
import SwiftUI

/// The two tabs, as on Android: Daily and Profile, switched with a floating toolbar rather than the
/// system tab bar. The toolbar only shows on the tabs' own screens, so pushed screens (an album,
/// Settings, Notifications) aren't covered by it.
struct RootView: View {
    let sdk: CassetteSdk

    @State private var tab = AppTab.daily
    @State private var dailyPath = NavigationPath()
    @State private var profilePath = NavigationPath()
    @State private var isShowingNowPlaying = false
    @Environment(DailyReminder.self) private var reminder

    private var isAtTabRoot: Bool {
        switch tab {
        case .daily: dailyPath.isEmpty
        case .profile: profilePath.isEmpty
        }
    }

    var body: some View {
        TabView(selection: $tab) {
            DailyView(sdk: sdk, path: $dailyPath)
                .toolbar(.hidden, for: .tabBar)
                .tag(AppTab.daily)
            ProfileView(path: $profilePath)
                .toolbar(.hidden, for: .tabBar)
                .tag(AppTab.profile)
        }
        // An inset rather than an overlay, so the tabs' scrolling content ends above the toolbar.
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if isAtTabRoot {
                FloatingToolbar(selection: $tab) { isShowingNowPlaying = true }
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(duration: 0.35, bounce: 0.15), value: isAtTabRoot)
        // Presented here, not from the toolbar's button, so it stays open after Stop, when the
        // button shrinks away.
        .sheet(isPresented: $isShowingNowPlaying) {
            NowPlayingSheet()
                .presentationDetents([.medium])
                .presentationDragIndicator(.visible)
        }
        // Tapping the daily reminder opens today's albums (DailyView handles the day itself).
        .onChange(of: reminder.openTodayRequests) { tab = .daily }
    }
}
