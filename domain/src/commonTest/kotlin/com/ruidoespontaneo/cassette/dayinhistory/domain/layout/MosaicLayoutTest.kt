package com.ruidoespontaneo.cassette.dayinhistory.domain.layout

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class MosaicLayoutTest {

    @Test
    fun `with nothing expanded covers fill rows of four in order`() {
        val cells = mosaicCells(count = 6, expandedIndex = null, columns = 4)

        assertEquals(
            listOf(
                MosaicCell(0, 0, 1), MosaicCell(0, 1, 1), MosaicCell(0, 2, 1), MosaicCell(0, 3, 1),
                MosaicCell(1, 0, 1), MosaicCell(1, 1, 1)
            ),
            cells
        )
        assertEquals(2, mosaicRowCount(cells))
    }

    @Test
    fun `an expanded cover takes two by two and the next covers fill beside it`() {
        val cells = mosaicCells(count = 5, expandedIndex = 0, columns = 4)

        assertEquals(MosaicCell(0, 0, 2), cells[0])
        assertEquals(
            listOf(MosaicCell(0, 2, 1), MosaicCell(0, 3, 1), MosaicCell(1, 2, 1), MosaicCell(1, 3, 1)),
            cells.drop(1)
        )
        assertEquals(2, mosaicRowCount(cells))
    }

    @Test
    fun `an expanded cover that would start in the last column drops down and the gap gets filled`() {
        val cells = mosaicCells(count = 6, expandedIndex = 3, columns = 4)

        assertEquals(MosaicCell(1, 0, 2), cells[3])
        // The next cover takes the spot the expanded one couldn't use.
        assertEquals(MosaicCell(0, 3, 1), cells[4])
        assertEquals(MosaicCell(1, 2, 1), cells[5])
        assertEquals(3, mosaicRowCount(cells))
    }

    @Test
    fun `every cover gets a cell and no two overlap`() {
        for (count in 0..12) {
            for (expanded in listOf<Int?>(null) + (0 until count)) {
                val cells = mosaicCells(count, expanded, columns = 4)
                assertEquals(count, cells.size)
                val tiles = cells.flatMap { cell ->
                    (0 until cell.span).flatMap { dr -> (0 until cell.span).map { dc -> (cell.row + dr) to (cell.column + dc) } }
                }
                assertEquals(tiles.size, tiles.toSet().size, "overlap with count=$count expanded=$expanded")
                assertTrue(tiles.all { (_, column) -> column in 0 until 4 })
            }
        }
    }

    @Test
    fun `no covers take no rows`() {
        assertEquals(0, mosaicRowCount(mosaicCells(count = 0, expandedIndex = null, columns = 4)))
    }
}
