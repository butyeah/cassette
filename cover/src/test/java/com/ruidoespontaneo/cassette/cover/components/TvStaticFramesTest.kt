package com.ruidoespontaneo.cassette.cover.components

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvStaticFramesTest {

    @Test
    fun `there are six frames of 64 by 64 grey levels`() {
        val frames = tvStaticFrames()

        assertEquals(6, frames.size)
        frames.forEach { frame ->
            assertEquals(64 * 64, frame.size)
            assertTrue(frame.all { it in 0..255 })
        }
    }

    @Test
    fun `the frames differ so the snow moves`() {
        val frames = tvStaticFrames()

        assertEquals(6, frames.map { it.toList() }.toSet().size)
    }

    @Test
    fun `the same seed draws the same snow`() {
        val a = tvStaticFrames(count = 2, size = 16, seed = 42)
        val b = tvStaticFrames(count = 2, size = 16, seed = 42)
        val other = tvStaticFrames(count = 2, size = 16, seed = 43)

        a.zip(b).forEach { (x, y) -> assertArrayEquals(x, y) }
        assertFalse(a[0].contentEquals(other[0]))
    }

    @Test
    fun `scanline rows are darker`() {
        val size = 64
        val frame = tvStaticFrames(count = 1, size = size).single()
        fun meanOfRows(parity: Int) = frame.filterIndexed { i, _ -> (i / size) % 2 == parity }.average()

        assertTrue(meanOfRows(1) < meanOfRows(0))
    }

    @Test
    fun `pixels are opaque greys`() {
        val pixels = intArrayOf(0, 128, 255).toArgbPixels()

        assertArrayEquals(intArrayOf(0xFF000000.toInt(), 0xFF808080.toInt(), 0xFFFFFFFF.toInt()), pixels)
    }

    // The first outputs of SplitMix64 for seed 0, from its reference implementation. iOS's
    // TVStaticTests checks the same numbers, so both apps draw the same snow.
    @Test
    fun `SplitMix64 matches the reference`() {
        val random = SplitMix64(0)

        assertEquals(0xE220A8397B1DCDAFuL.toLong(), random.next())
        assertEquals(0x6E789E6AA1B965F4L, random.next())
    }
}
