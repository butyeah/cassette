package com.ruidoespontaneo.cassette.itunes.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TitleNormalizerTest {

    @Test
    fun `ignores case and punctuation`() {
        assertEquals("newborn", "New Born!".normalizedForMatching())
        assertEquals("dontstopmenow", "Don't Stop Me Now".normalizedForMatching())
    }

    @Test
    fun `drops bracketed decorations`() {
        assertEquals("song", "Song (feat. Someone)".normalizedForMatching())
        assertEquals("song", "Song [Remastered]".normalizedForMatching())
        assertEquals("inbetweendreams", "In Between Dreams (Bonus Track Version)".normalizedForMatching())
    }

    @Test
    fun `drops a dash suffix`() {
        assertEquals("song", "Song - Remastered 2009".normalizedForMatching())
    }

    @Test
    fun `keeps a hyphen inside a word`() {
        assertEquals("spiderman", "Spider-Man".normalizedForMatching())
    }

    @Test
    fun `keeps non-latin letters`() {
        assertEquals("ключ", "Ключ".normalizedForMatching())
    }

    @Test
    fun `keeps numbers in any form`() {
        // Fullwidth digits, Roman numerals and superscripts are numbers too (\p{N}).
        assertEquals("symphony５", "Symphony ５".normalizedForMatching())
        assertEquals("rockyⅳ", "Rocky Ⅳ".normalizedForMatching())
        assertEquals("emc²", "E＝mc²".normalizedForMatching())
    }

    @Test
    fun `a title that is entirely a parenthetical still compares equal to itself`() {
        assertEquals("untitled", "(Untitled)".normalizedForMatching())
        assertNotEquals("(Untitled)".normalizedForMatching(), "(Other)".normalizedForMatching())
    }
}
