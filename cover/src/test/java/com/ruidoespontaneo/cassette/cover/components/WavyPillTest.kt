package com.ruidoespontaneo.cassette.cover.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavyPillTest {

    private val start = 24f
    private val end = 324f
    private val wavelength = 40f

    private fun offset(x: Float, amplitude: Float = 6f, phase: Float = 0f) =
        wavyEdgeOffset(x, start, end, amplitude, wavelength, phase)

    @Test
    fun `the wave is flat where it meets the round caps`() {
        assertEquals(0f, offset(start), 0f)
        assertEquals(0f, offset(end), 0f)
        assertEquals(0f, offset(start - 10f), 0f)
        assertEquals(0f, offset(end + 10f), 0f)
    }

    @Test
    fun `the wave tapers in near the caps`() {
        // A crest just inside the taper swings less than the same crest in the middle.
        val nearCap = offset(start + 10f, phase = start)
        val middle = offset(start + 10f + wavelength * 3, phase = start)
        assertTrue(kotlin.math.abs(nearCap) < kotlin.math.abs(middle))
    }

    @Test
    fun `the wave never swings beyond its amplitude`() {
        var x = start
        while (x <= end) {
            assertTrue(kotlin.math.abs(offset(x)) <= 6f + 1e-4f)
            x += 0.5f
        }
    }

    @Test
    fun `zero amplitude is a plain pill`() {
        var x = start
        while (x <= end) {
            assertEquals(0f, offset(x, amplitude = 0f), 0f)
            x += 5f
        }
    }

    @Test
    fun `the phase moves the wave along`() {
        val x = (start + end) / 2
        assertNotEquals(offset(x, phase = 0f), offset(x, phase = wavelength / 4), 1e-3f)
        assertEquals(offset(x, phase = 0f), offset(x, phase = wavelength), 1e-3f)
    }
}
