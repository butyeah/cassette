package com.ruidoespontaneo.cassette.albumdetail.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseStoryTextTest {

    @Test
    fun `names are joined with the language's and before the last`() {
        assertEquals("", joinNames(emptyList(), "%1\$s y %2\$s"))
        assertEquals("Parlophone", joinNames(listOf("Parlophone"), "%1\$s y %2\$s"))
        assertEquals("Parlophone y Capitol", joinNames(listOf("Parlophone", "Capitol"), "%1\$s y %2\$s"))
        assertEquals("Parlophone, Capitol and EMI", joinNames(listOf("Parlophone", "Capitol", "EMI"), "%1\$s and %2\$s"))
    }
}
