package com.ruidoespontaneo.cassette.albumdetail.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreTileMotionTest {

    private val width = 400f
    private val square = 10f
    private val tile = 30f
    private val gap = 20f

    @Test
    fun `centre squares rest spread evenly across the strip`() {
        // Four squares: centred at 50, 150, 250 and 350.
        assertEquals(listOf(45f, 145f, 245f, 345f), (0 until 4).map { GenreTileMotion.restX(it, 4, width, square) })
    }

    @Test
    fun `each square leaves by its nearer edge`() {
        assertEquals(-20f, GenreTileMotion.edgeX(0, 4, width, square))
        assertEquals(-20f, GenreTileMotion.edgeX(1, 4, width, square))
        assertEquals(410f, GenreTileMotion.edgeX(2, 4, width, square))
        assertEquals(410f, GenreTileMotion.edgeX(3, 4, width, square))
    }

    @Test
    fun `tiles wait off the right edge, then roll in one after another`() {
        // Nothing travelled: the first tile is at the right edge, the next one a tile and a gap behind.
        assertEquals(400f, GenreTileMotion.loopX(0, 4, 0f, width, tile, gap))
        assertEquals(450f, GenreTileMotion.loopX(1, 4, 0f, width, tile, gap))
        assertEquals(300f, GenreTileMotion.loopX(0, 4, 100f, width, tile, gap))
        assertEquals(350f, GenreTileMotion.loopX(1, 4, 100f, width, tile, gap))
    }

    @Test
    fun `a tile comes back in on the right only once it's out of sight on the left`() {
        // The loop is the strip plus a tile, 430: just before wrapping the tile is fully off the left.
        val beforeWrap = GenreTileMotion.loopX(0, 4, 429f, width, tile, gap)
        val afterWrap = GenreTileMotion.loopX(0, 4, 431f, width, tile, gap)
        assertTrue(beforeWrap < -tile + 2)
        assertEquals(399f, afterWrap, 0.001f)
    }

    @Test
    fun `rolling turns as far as a wheel would`() {
        // A 10-wide square rolling its own circumference, 10π, turns once.
        assertEquals(360f, GenreTileMotion.rollingDegrees((10 * Math.PI).toFloat(), 10f), 0.01f)
        assertEquals(-180f, GenreTileMotion.rollingDegrees((-5 * Math.PI).toFloat(), 10f), 0.01f)
    }
}
