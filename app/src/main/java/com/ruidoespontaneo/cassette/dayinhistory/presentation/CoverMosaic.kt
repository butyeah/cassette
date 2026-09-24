package com.ruidoespontaneo.cassette.dayinhistory.presentation

/** Where one cover sits in the Daily grid's mosaic: its top-left tile and how many tiles wide/tall. */
data class MosaicCell(val row: Int, val column: Int, val span: Int)

/** How many tiles wide/tall the expanded cover is. */
const val EXPANDED_SPAN = 2

/**
 * Packs [count] covers into rows of [columns] tiles, in order: every cover takes one tile, except
 * the one at [expandedIndex], which takes [EXPANDED_SPAN]×[EXPANDED_SPAN]. Dense first-fit — each
 * cover goes in the first free spot it fits, scanning row by row — so when the expanded cover has
 * to drop to the next row, later covers fill the gap it left, and the rest of the grid flows
 * around it. Returns one cell per cover, by index.
 */
fun mosaicCells(count: Int, expandedIndex: Int?, columns: Int): List<MosaicCell> {
    require(columns > 0) { "columns must be positive" }
    val occupied = mutableListOf<BooleanArray>()
    fun isFree(row: Int, column: Int) = row >= occupied.size || !occupied[row][column]
    fun occupy(row: Int, column: Int) {
        while (occupied.size <= row) occupied += BooleanArray(columns)
        occupied[row][column] = true
    }

    return List(count) { index ->
        val span = if (index == expandedIndex) EXPANDED_SPAN.coerceAtMost(columns) else 1
        var row = 0
        var column = 0
        while (true) {
            val fits = column + span <= columns &&
                (0 until span).all { dr -> (0 until span).all { dc -> isFree(row + dr, column + dc) } }
            if (fits) break
            column++
            if (column + span > columns) {
                column = 0
                row++
            }
        }
        for (dr in 0 until span) for (dc in 0 until span) occupy(row + dr, column + dc)
        MosaicCell(row, column, span)
    }
}

/** How many tile rows [cells] take up. */
fun mosaicRowCount(cells: List<MosaicCell>): Int = cells.maxOfOrNull { it.row + it.span } ?: 0
