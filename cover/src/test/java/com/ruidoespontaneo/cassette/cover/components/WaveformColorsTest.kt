package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformColorsTest {

    private val lightBackground = Color(0xFFFFFBFE)
    private val darkBackground = Color(0xFF1C1B1F)

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
        val album = listOf(Color(0xFF7A0019))
        val fallback = listOf(Color.Red, Color.Green, Color.Blue)

        val result = waveformColors(album, fallback, lightBackground, count = 3)

        assertEquals(3, result.size)
        assertEquals(album.single(), result[0])
        assertEquals(Color.Green, result[1])
        assertEquals(Color.Blue, result[2])
    }

    @Test
    fun `waveformColors is all fallback until the cover has decoded`() {
        val fallback = listOf(Color.Red, Color.Green, Color.Blue)

        assertEquals(fallback, waveformColors(null, fallback, lightBackground, count = 3))
    }

    @Test
    fun `waveformColors caps at count and cycles a short fallback`() {
        val tooMany = List(5) { Color(0xFF000000) }

        assertEquals(3, waveformColors(tooMany, listOf(Color.Red), lightBackground, count = 3).size)
        assertEquals(
            listOf(Color.Red, Color.Red, Color.Red),
            waveformColors(null, listOf(Color.Red), lightBackground, count = 3)
        )
    }
}
