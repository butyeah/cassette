import CoreGraphics

/// Where the genre tiles and their centre squares sit as they roll along `GenreTileStrip`, in
/// points along the strip. A port of Android's GenreTileMotion.kt, kept apart from the drawing so it
/// can be tested, with explicit types so device builds type-check it in time.
enum GenreTileMotion {

    /// At rest, the `index`th centre square of `count`, spread evenly across `width`. Its left edge.
    static func restX(index: Int, count: Int, width: CGFloat, square: CGFloat) -> CGFloat {
        width * (CGFloat(index) + 0.5) / CGFloat(count) - square / 2
    }

    /// Just past whichever edge the `index`th centre square is nearer to, where it rolls out to.
    static func edgeX(index: Int, count: Int, width: CGFloat, square: CGFloat) -> CGFloat {
        let rest: CGFloat = restX(index: index, count: count, width: width, square: square)
        return rest + square / 2 < width / 2 ? -square * 2 : width + square
    }

    /// The `index`th tile's left edge after the row has rolled `travelled` points leftwards. Each
    /// tile waits off the right edge until its turn, then crosses and comes back in on the right,
    /// `gap` behind the one before it.
    static func loopX(index: Int, count: Int, travelled: CGFloat, width: CGFloat, tile: CGFloat, gap: CGFloat) -> CGFloat {
        let spacing: CGFloat = tile + gap
        // At least the strip plus a tile, so a tile only wraps round once it's out of sight.
        let loopLength: CGFloat = max(width + tile, CGFloat(count) * spacing)
        let along: CGFloat = travelled - CGFloat(index) * spacing
        return along < 0 ? width - along : width - along.truncatingRemainder(dividingBy: loopLength)
    }

    /// How far something `size` across has turned, in degrees, after rolling `distance` points
    /// without slipping: a wheel turns distance / radius radians. Positive is clockwise, rolling right.
    static func rollingDegrees(distance: CGFloat, size: CGFloat) -> CGFloat {
        distance / (size / 2) * 180 / .pi
    }

    /// Ease in and out, as Android's cubic-bezier(0.45, 0, 0.55, 1) roughly does.
    static func eased(_ t: Double) -> Double {
        let clamped: Double = min(max(t, 0), 1)
        return clamped * clamped * (3 - 2 * clamped)
    }
}
