package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformColorsTest {

    private val lightBackground = Color(0xFFFFFBFE)
    private val darkBackground = Color(0xFF1C1B1F)

    @Test
    fun `displayTextColor is white until the cover has decoded`() {
        assertEquals(Color.White, displayTextColor(dominant = null, background = Color.Black))
        assertEquals(Color.White, displayTextColor(dominant = emptyList(), background = Color.Black))
    }

    @Test
    fun `displayTextColor uses the most dominant color, made readable as text`() {
        val murky = Color(0xFF2A2830)

        val result = displayTextColor(dominant = listOf(murky, Color.White), background = Color.Black)

        assertTrue(contrastRatio(result, Color.Black) >= 4.5f)
        assertTrue(result != Color.White)
    }

    @Test
    fun `a color that already contrasts enough is left alone`() {
        assertEquals(Color.Black, readableOn(lightBackground, Color.Black))
    }

    @Test
    fun `a washed-out color on a light background is darkened until it reads`() {
        val pale = Color(0xFFE8DEF8)

        val result = readableOn(lightBackground, pale)

        assertTrue(contrastRatio(result, lightBackground) >= 3f)
    }

    @Test
    fun `a murky color on a dark background is lightened until it reads`() {
        val murky = Color(0xFF2A2830)

        val result = readableOn(darkBackground, murky)

        assertTrue(contrastRatio(result, darkBackground) >= 3f)
    }

    @Test
    fun `the color that can never reach the ratio falls back to the extreme`() {
        assertEquals(Color.Black, readableOn(lightBackground, lightBackground, minContrast = 21f))
    }

    @Test
    fun `waveformColors uses the album colors first and tops up from the fallback`() {
        val album = listOf(Color(0xFFFFC107))
        val fallback = listOf(Color.Red, Color.Green, Color.Yellow)

        val result = waveformColors(album, fallback, Color.Black, count = 3)

        assertEquals(listOf(album.single(), Color.Green, Color.Yellow), result)
    }

    @Test
    fun `waveformColors is all fallback until the cover has decoded`() {
        val fallback = listOf(Color.Red, Color.Green, Color.Yellow)

        assertEquals(fallback, waveformColors(null, fallback, Color.Black, count = 3))
    }

    @Test
    fun `waveformColors caps at count and cycles a short fallback`() {
        val tooMany = List(5) { Color(0xFFFFC107) }

        assertEquals(3, waveformColors(tooMany, listOf(Color.Red), Color.Black, count = 3).size)
        assertEquals(
            listOf(Color.Red, Color.Red, Color.Red),
            waveformColors(null, listOf(Color.Red), Color.Black, count = 3)
        )
    }

    @Test
    fun `fallback colors are made readable on the background too`() {
        val darkPurple = Color(0xFF3B2A6B)

        val result = waveformColors(null, listOf(darkPurple), Color.Black, count = 1)

        assertTrue(contrastRatio(result.single(), Color.Black) >= 3f)
    }

    @Test
    fun `highlightContainerColors needs at least two dominant colors`() {
        assertEquals(null, highlightContainerColors(null))
        assertEquals(null, highlightContainerColors(listOf(Color.Red)))
    }

    @Test
    fun `highlightContainerColors picks the dominant color that stands out most from the backdrop`() {
        val backdrop = Color(0xFF202020)
        val similar = Color(0xFF303030)
        val standout = Color(0xFFF0E68C)
        assertEquals(standout, highlightContainerColors(listOf(backdrop, similar, standout))?.container)
    }

    @Test
    fun `highlightContainerColors uses dark content on a light container and light on a dark one`() {
        assertEquals(Color.Black, highlightContainerColors(listOf(Color.Black, Color(0xFFF0E68C)))?.content)
        assertEquals(Color.White, highlightContainerColors(listOf(Color.White, Color(0xFF1A237E)))?.content)
    }
}
