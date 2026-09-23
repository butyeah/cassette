package com.ruidoespontaneo.cassette.albumdetail.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewDisplayFormattingTest {

    @Test
    fun `unknown remaining time shows dashes`() {
        assertEquals("--:--", remainingTimeText(null))
    }

    @Test
    fun `whole seconds are shown as is`() {
        assertEquals("00:24", remainingTimeText(24_000))
    }

    @Test
    fun `a partial second rounds up`() {
        assertEquals("00:24", remainingTimeText(23_100))
    }

    @Test
    fun `a finished clip shows zero`() {
        assertEquals("00:00", remainingTimeText(0))
    }

    @Test
    fun `minutes roll over`() {
        assertEquals("01:05", remainingTimeText(65_000))
    }
}
