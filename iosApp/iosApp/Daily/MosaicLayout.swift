import Shared
import SwiftUI

/// Where one cover sits in the grid: its top-left tile and how many tiles wide and tall it is.
struct MosaicTile: Equatable {
    let row: Int
    let column: Int
    let span: Int
}

let mosaicColumns = 4

/// The shared `mosaicCells` (domain/.../dayinhistory/domain/layout), the same packing Android's
/// Daily grid uses: every cover takes one tile except the one at `expandedIndex`, which takes 2×2,
/// and later covers fill any gap it leaves.
func mosaicCells(count: Int, expandedIndex: Int?) -> [MosaicTile] {
    MosaicLayoutKt.mosaicCells(
        count: Int32(count),
        expandedIndex: expandedIndex.map { KotlinInt(int: Int32($0)) },
        columns: Int32(mosaicColumns)
    ).map { MosaicTile(row: Int($0.row), column: Int($0.column), span: Int($0.span)) }
}

/// Places its subviews, in order, on square tiles by `cells`, `gap` apart. It's as wide as it's
/// offered and as tall as its rows. Changing `cells` inside an animation moves every cover to its
/// new place, and grows or shrinks the grid, on that animation.
struct MosaicLayout: Layout {
    let cells: [MosaicTile]
    var gap: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        let rows = cells.map { $0.row + $0.span }.max() ?? 0
        let height = rows == 0 ? 0 : CGFloat(rows) * tileSize(width) + CGFloat(rows - 1) * gap
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let tile = tileSize(bounds.width)
        for (subview, cell) in zip(subviews, cells) {
            let side = tile * CGFloat(cell.span) + gap * CGFloat(cell.span - 1)
            subview.place(
                at: CGPoint(
                    x: bounds.minX + CGFloat(cell.column) * (tile + gap),
                    y: bounds.minY + CGFloat(cell.row) * (tile + gap)
                ),
                proposal: ProposedViewSize(width: side, height: side)
            )
        }
    }

    private func tileSize(_ width: CGFloat) -> CGFloat {
        max((width - gap * CGFloat(mosaicColumns - 1)) / CGFloat(mosaicColumns), 0)
    }
}
