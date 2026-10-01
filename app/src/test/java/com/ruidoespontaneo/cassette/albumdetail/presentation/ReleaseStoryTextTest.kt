package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseStoryTextTest {

    private fun AnnotatedString.boldParts() =
        spanStyles.filter { it.item.fontWeight == FontWeight.Bold }.map { text.substring(it.start, it.end) }

    @Test
    fun `fills each argument in its place, in bold`() {
        val text = boldArguments(
            pattern = "Este disco fue autoproducido por %4\$s y liberado el %1\$s bajo el sello %2\$s.",
            arguments = listOf(listOf("21 de mayo de 1997"), listOf("Parlophone"), emptyList(), listOf("Radiohead")),
            and = "%1\$s y %2\$s"
        )

        assertEquals("Este disco fue autoproducido por Radiohead y liberado el 21 de mayo de 1997 bajo el sello Parlophone.", text.text)
        assertEquals(listOf("Radiohead", "21 de mayo de 1997", "Parlophone"), text.boldParts())
    }

    @Test
    fun `joins several names with the language's and, only the names in bold`() {
        val two = boldArguments("%2\$s", listOf(emptyList(), listOf("Parlophone", "Capitol")), and = "%1\$s y %2\$s")
        val three = boldArguments("%2\$s", listOf(emptyList(), listOf("Parlophone", "Capitol", "EMI")), and = "%1\$s and %2\$s")

        assertEquals("Parlophone y Capitol", two.text)
        assertEquals(listOf("Parlophone", "Capitol"), two.boldParts())
        assertEquals("Parlophone, Capitol and EMI", three.text)
        assertEquals(listOf("Parlophone", "Capitol", "EMI"), three.boldParts())
    }
}
