import SwiftUI

/// One line of text that, when it's too long to fit, scrolls across every `interval` seconds, as
/// Android's `basicMarquee` does with the same settings: it waits `interval`, scrolls one full
/// loop at `velocity` points per second with a copy following a third of the width behind, then
/// waits again. A new `text` starts it over from the wait. Text that fits, or Reduce Motion, keeps
/// it still, cut off with "…".
struct MarqueeText: View {
    let text: String
    var interval: Double = 10
    var velocity: CGFloat = 30

    @State private var textWidth: CGFloat = 0
    @State private var containerWidth: CGFloat = 0
    @State private var offset: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(_ text: String, interval: Double = 10, velocity: CGFloat = 30) {
        self.text = text
        self.interval = interval
        self.velocity = velocity
    }

    /// The gap between the text and the copy that follows it, as `basicMarquee`'s default spacing.
    private var gap: CGFloat { containerWidth / 3 }
    private var scrolls: Bool { !reduceMotion && textWidth > containerWidth && containerWidth > 0 }

    var body: some View {
        // The plain, cut-off line sizes the view and is what shows when it doesn't scroll.
        Text(text)
            .lineLimit(1)
            .opacity(scrolls ? 0 : 1)
            .frame(maxWidth: .infinity, alignment: .leading)
            .onGeometryChange(for: CGFloat.self, of: \.size.width) { containerWidth = $0 }
            .overlay(alignment: .leading) {
                if scrolls {
                    HStack(spacing: gap) {
                        Text(text)
                        Text(text)
                    }
                    .fixedSize()
                    .offset(x: offset)
                }
            }
            .background(alignment: .leading) {
                // Measures the whole line, unconstrained.
                Text(text)
                    .fixedSize()
                    .hidden()
                    .onGeometryChange(for: CGFloat.self, of: \.size.width) { textWidth = $0 }
            }
            .clipped()
            // One element reading the text once, however many copies are drawn.
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(text)
            .task(id: [text, String(describing: scrolls), String(describing: containerWidth)]) {
                await run()
            }
    }

    private func run() async {
        offset = 0
        guard scrolls else { return }
        let distance = textWidth + gap
        let duration = Double(distance / velocity)
        while !Task.isCancelled {
            try? await Task.sleep(for: .seconds(interval))
            guard !Task.isCancelled else { return }
            withAnimation(.linear(duration: duration)) { offset = -distance }
            try? await Task.sleep(for: .seconds(duration))
            guard !Task.isCancelled else { return }
            // The copy now sits exactly where the text started, so jumping back is seamless.
            offset = 0
        }
    }
}
