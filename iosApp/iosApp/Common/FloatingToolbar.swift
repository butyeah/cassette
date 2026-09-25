import SwiftUI

/// The app's top-level sections.
enum AppTab: Hashable, CaseIterable {
    case daily, profile

    var title: LocalizedStringKey {
        switch self {
        case .daily: "Daily"
        case .profile: "Profile"
        }
    }

    var systemImage: String {
        switch self {
        case .daily: "house"
        case .profile: "person"
        }
    }
}

/// A floating pill holding the app's tabs, in place of the system tab bar. A port of Android's
/// CassetteFloatingToolbar.
///
/// While a clip is buffering or playing, a round button with the playing album's cover floats to its
/// left, and `onNowPlayingTap` opens the now-playing sheet.
struct FloatingToolbar: View {
    @Binding var selection: AppTab
    let onNowPlayingTap: () -> Void

    @Environment(PreviewPlayer.self) private var player

    var body: some View {
        FloatingSurfaceGroup {
            HStack(spacing: 8) {
                // Beside the toolbar, not in it, as on Android.
                if player.status != .stopped, let nowPlaying = player.nowPlaying {
                    NowPlayingBubble(nowPlaying: nowPlaying, isLoading: player.status == .loading, onTap: onNowPlayingTap)
                        .transition(.scale.combined(with: .opacity))
                }
                HStack(spacing: 4) {
                    ForEach(AppTab.allCases, id: \.self) { tab in
                        TabButton(tab: tab, isSelected: selection == tab) { selection = tab }
                    }
                }
                .padding(6)
                .floatingSurface(in: Capsule())
            }
        }
        .animation(.spring(duration: 0.4, bounce: 0.25), value: player.status == .stopped)
    }
}

private struct TabButton: View {
    let tab: AppTab
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            // The label right beside it already names the tab.
            Label(tab.title, systemImage: tab.systemImage)
                .font(.pixel(15, weight: .medium))
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .foregroundStyle(isSelected ? AnyShapeStyle(.tint) : AnyShapeStyle(.primary))
                .background {
                    // Android's filled tonal button, for the selected tab.
                    if isSelected {
                        Capsule().fill(.tint.opacity(0.18))
                    }
                }
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// The playing album's cover in a circle: the button itself, no label. Every 10 s it grows a little
/// and settles back, a quiet sign that something is playing, as on Android.
private struct NowPlayingBubble: View {
    let nowPlaying: NowPlaying
    let isLoading: Bool
    let onTap: () -> Void

    @State private var scale: CGFloat = 1
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let size: CGFloat = 56
    private static let pulseInterval: Duration = .seconds(10)
    private static let pulseScale: CGFloat = 1.12

    var body: some View {
        Button(action: onTap) {
            CoverImage(url: nowPlaying.album.coverURL, cornerRadius: Self.size / 2)
                .frame(width: Self.size)
                .overlay {
                    if isLoading {
                        ProgressView().tint(.white)
                    }
                }
                .padding(4)
                .floatingSurface(in: Circle())
        }
        .buttonStyle(.plain)
        .scaleEffect(scale)
        .accessibilityLabel("Now playing: \(nowPlaying.trackTitle ?? nowPlaying.album.title)")
        .task(id: reduceMotion) {
            guard !reduceMotion else { return }
            while !Task.isCancelled {
                try? await Task.sleep(for: Self.pulseInterval)
                guard !Task.isCancelled else { return }
                withAnimation(.spring(duration: 0.25, bounce: 0.3)) { scale = Self.pulseScale }
                try? await Task.sleep(for: .seconds(0.25))
                withAnimation(.spring(duration: 0.5, bounce: 0.3)) { scale = 1 }
            }
        }
    }
}

extension View {
    /// What the toolbar's pieces float on: Liquid Glass on iOS 26, frosted material with a shadow
    /// before it.
    @ViewBuilder
    func floatingSurface(in shape: some Shape) -> some View {
        if #available(iOS 26, *) {
            glassEffect(.regular.interactive(), in: shape)
        } else {
            background(.regularMaterial, in: shape)
                .shadow(color: .black.opacity(0.18), radius: 8, y: 3)
        }
    }
}

/// On iOS 26, groups glass pieces so they render, and blend as they move, as one set.
private struct FloatingSurfaceGroup<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: 8) { content }
        } else {
            content
        }
    }
}
