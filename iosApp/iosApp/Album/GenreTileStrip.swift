import Shared
import SwiftUI

/// A strip of `tiles`, one per genre of the album, along the bottom of the player. A port of
/// Android's GenreTileStrip: while nothing of the album is `playing`, only each tile's centre square
/// shows, spread across the strip. When it starts, each square rolls out by its nearer edge, then the
/// whole tiles roll in from the right and keep rolling round. When it stops, the tiles fade and the
/// squares roll back to their places.
///
/// Everything rolls like a wheel, turning as far as it travels. With Reduce Motion on, it switches
/// between still squares and still tiles instead. Decorative: the genres are read out below.
///
/// Not shown for now: the release story sits where it would go, along the bottom of the player.
/// Shown with `sdk.genreTiles(genres: album.genres)` and `playing: activePosition != nil`.
struct GenreTileStrip: View {
    let tiles: [GenreTile]
    let playing: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// When `playing` last changed, and how far out the squares were then (0 at rest, 1 past the
    /// edges), so a change halfway through a roll carries on from where it was.
    @State private var changedAt = Date.distantPast
    @State private var squaresOutAtChange: Double = 0

    private static let tile: CGFloat = 30
    private static let gap: CGFloat = 20
    private static let height: CGFloat = 52
    private static let speed: CGFloat = 45 // points per second
    private static let rollDuration: Double = 0.9
    private static let fadeDuration: Double = 0.25

    var body: some View {
        if !tiles.isEmpty {
            TimelineView(.animation(paused: reduceMotion)) { timeline in
                let frame = Frame(at: timeline.date, playing: playing, changedAt: changedAt, from: squaresOutAtChange, still: reduceMotion)
                Canvas { context, size in
                    draw(frame, in: context, width: size.width)
                }
            }
            .frame(height: Self.height)
            .onChange(of: playing) {
                let now = Date()
                squaresOutAtChange = Frame(at: now, playing: !playing, changedAt: changedAt, from: squaresOutAtChange, still: reduceMotion).squaresOut
                changedAt = now
            }
            .accessibilityHidden(true)
        }
    }

    private var palettes: [[Color]] {
        tiles.map { tile in
            tile.colors.map { RGB(UInt32(truncatingIfNeeded: $0.int64Value)).color }
        }
    }

    /// Where everything is at one moment.
    private struct Frame {
        /// 0 with the squares at rest, 1 with them out past the edges.
        let squaresOut: Double
        let showsSquares: Bool
        let showsTiles: Bool
        let tilesOpacity: Double
        /// How far the tiles have rolled since they started.
        let travelled: CGFloat

        init(at now: Date, playing: Bool, changedAt: Date, from start: Double, still: Bool) {
            let elapsed: Double = now.timeIntervalSince(changedAt)
            if still {
                squaresOut = playing ? 1 : 0
                showsSquares = !playing
                showsTiles = playing
                tilesOpacity = 1
                travelled = 0
                return
            }
            // How far through the roll, scaled by how far there was left to go.
            let distance: Double = playing ? 1 - start : start
            let duration: Double = GenreTileStrip.rollDuration * max(distance, 0.001)
            let progress: Double = GenreTileMotion.eased(elapsed / duration)
            if playing {
                squaresOut = start + (1 - start) * progress
                let rolledOut: Bool = elapsed >= duration
                showsSquares = !rolledOut
                showsTiles = rolledOut
                tilesOpacity = 1
                travelled = rolledOut ? CGFloat(elapsed - duration) * GenreTileStrip.speed : 0
            } else {
                squaresOut = start * (1 - progress)
                showsSquares = true
                let fade: Double = min(elapsed / GenreTileStrip.fadeDuration, 1)
                // The tiles only fade if they were out, rolling, when it stopped.
                showsTiles = start >= 1 && fade < 1
                tilesOpacity = 1 - fade
                travelled = CGFloat(elapsed) * GenreTileStrip.speed
            }
        }
    }

    private func draw(_ frame: Frame, in context: GraphicsContext, width: CGFloat) {
        let count: Int = tiles.count
        let square: CGFloat = Self.tile / 3
        let palettes = self.palettes
        if frame.showsSquares {
            for index in 0..<count {
                let rest: CGFloat = GenreTileMotion.restX(index: index, count: count, width: width, square: square)
                let edge: CGFloat = GenreTileMotion.edgeX(index: index, count: count, width: width, square: square)
                let x: CGFloat = rest + (edge - rest) * CGFloat(frame.squaresOut)
                let degrees: CGFloat = GenreTileMotion.rollingDegrees(distance: x - rest, size: square)
                drawRolling(in: context, x: x, side: square, degrees: degrees) { inner in
                    inner.fill(Path(CGRect(x: 0, y: 0, width: square, height: square)), with: .color(palettes[index][2]))
                }
            }
        }
        if frame.showsTiles {
            for index in 0..<count {
                let x: CGFloat = reduceMotion
                    ? GenreTileMotion.restX(index: index, count: count, width: width, square: Self.tile)
                    : GenreTileMotion.loopX(index: index, count: count, travelled: frame.travelled, width: width, tile: Self.tile, gap: Self.gap)
                let degrees: CGFloat = GenreTileMotion.rollingDegrees(distance: x, size: Self.tile)
                drawRolling(in: context, x: x, side: Self.tile, degrees: degrees) { inner in
                    var faded = inner
                    faded.opacity = frame.tilesOpacity
                    drawX(palettes[index], in: faded)
                }
            }
        }
    }

    /// Runs `draw` for something `side` square at `x`, vertically centred, turned `degrees` about
    /// its centre.
    private func drawRolling(in context: GraphicsContext, x: CGFloat, side: CGFloat, degrees: CGFloat, draw: (GraphicsContext) -> Void) {
        var inner = context
        let top: CGFloat = (Self.height - side) / 2
        inner.translateBy(x: x + side / 2, y: top + side / 2)
        inner.rotate(by: .degrees(Double(degrees)))
        inner.translateBy(x: -side / 2, y: -side / 2)
        draw(inner)
    }

    /// The five squares of a tile: corners and centre of a 3×3 grid, in `GenreTile.colors`' order.
    private func drawX(_ palette: [Color], in context: GraphicsContext) {
        let cell: CGFloat = Self.tile / 3
        let cells: [(column: CGFloat, row: CGFloat)] = [(0, 0), (2, 0), (1, 1), (0, 2), (2, 2)]
        for (index, position) in cells.enumerated() {
            let rect = CGRect(x: position.column * cell, y: position.row * cell, width: cell, height: cell)
            context.fill(Path(rect), with: .color(palette[index]))
        }
    }
}
