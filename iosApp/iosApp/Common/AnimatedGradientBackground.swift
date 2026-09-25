import SwiftUI

/// Three soft, blurred colour blobs drifting in slow, independent orbits: a "living" gradient to
/// sit behind a screen's content. A port of Android's AnimatedGradientBackground, with the same
/// anchors, orbits, sizes and blur.
///
/// `colors` overrides the three blobs' colours, most dominant first (an album's own cover
/// colours, say). Left `nil`, or shorter than three, falls back to the palette per missing slot, so
/// a partial list is never wrong, just less colourful.
///
/// Holds still when Reduce Motion is on.
struct AnimatedGradientBackground: View {
    var colors: [Color]?

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private struct Blob {
        /// Where the orbit is centred, as a fraction of the size.
        let anchor: CGPoint
        /// Seconds per full orbit. Different for each blob, so the pattern never repeats exactly.
        let period: Double
    }

    private static let blobs = [
        Blob(anchor: CGPoint(x: 0.3, y: 0.3), period: 9),
        Blob(anchor: CGPoint(x: 0.7, y: 0.5), period: 13),
        Blob(anchor: CGPoint(x: 0.5, y: 0.8), period: 17),
    ]
    private static let paletteColors: [Color] = [.palettePrimary, .paletteSecondary, .paletteTertiary]

    /// How far each blob swings from its anchor, as a fraction of the size.
    private static let orbitAmplitude = 0.22
    private static let blobRadiusFraction = 0.6
    private static let blobAlpha = 0.75
    private static let blurRadius: CGFloat = 50

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { timeline in
            let time = timeline.date.timeIntervalSinceReferenceDate
            Canvas { context, size in
                let radius = min(size.width, size.height) * Self.blobRadiusFraction
                for (index, blob) in Self.blobs.enumerated() {
                    let angle = time.truncatingRemainder(dividingBy: blob.period) / blob.period * 2 * .pi
                    let center = CGPoint(
                        x: size.width * blob.anchor.x + cos(angle) * size.width * Self.orbitAmplitude,
                        y: size.height * blob.anchor.y + sin(angle) * size.height * Self.orbitAmplitude
                    )
                    let color = colors?[safe: index] ?? Self.paletteColors[index]
                    context.fill(
                        Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)),
                        with: .radialGradient(
                            Gradient(colors: [color.opacity(Self.blobAlpha), color.opacity(0)]),
                            center: center,
                            startRadius: 0,
                            endRadius: radius
                        )
                    )
                }
            }
            .blur(radius: Self.blurRadius)
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
