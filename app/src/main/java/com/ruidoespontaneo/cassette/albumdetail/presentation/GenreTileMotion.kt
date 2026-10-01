package com.ruidoespontaneo.cassette.albumdetail.presentation

import kotlin.math.PI
import kotlin.math.max

/**
 * Where the genre tiles and their centre squares sit as they roll along [GenreTileStrip], in
 * pixels along the strip. Kept apart from the drawing so it can be tested.
 */
internal object GenreTileMotion {

    /** At rest, the [index]th centre square of [count], spread evenly across [width]. Its left edge. */
    fun restX(index: Int, count: Int, width: Float, square: Float): Float =
        width * (index + 0.5f) / count - square / 2

    /** Just past whichever edge the [index]th centre square is nearer to, where it rolls out to. */
    fun edgeX(index: Int, count: Int, width: Float, square: Float): Float =
        if (restX(index, count, width, square) + square / 2 < width / 2) -square * 2 else width + square

    /**
     * The [index]th tile's left edge after the row has rolled [travelled] pixels leftwards. Each
     * tile waits off the right edge until its turn, then crosses and comes back in on the right,
     * [gap] behind the one before it.
     */
    fun loopX(index: Int, count: Int, travelled: Float, width: Float, tile: Float, gap: Float): Float {
        val spacing = tile + gap
        // At least the strip plus a tile, so a tile only wraps round once it's out of sight.
        val loopLength = max(width + tile, count * spacing)
        val along = travelled - index * spacing
        return if (along < 0) width - along else width - along % loopLength
    }

    /**
     * How far something [size] across has turned, in degrees, after rolling [distance] pixels
     * without slipping: a wheel turns distance / radius radians. Positive is clockwise, rolling
     * right.
     */
    fun rollingDegrees(distance: Float, size: Float): Float = (distance / (size / 2) * 180 / PI).toFloat()
}
