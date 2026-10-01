package com.ruidoespontaneo.cassette.musicbrainz.domain.genre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreTilesTest {

    @Test
    fun `each family is found by its words`() {
        mapOf(
            "alternative rock" to GenreFamily.Rock,
            "shoegaze" to GenreFamily.Rock,
            "dance-pop" to GenreFamily.Pop,
            "techno" to GenreFamily.Electronic,
            "dubstep" to GenreFamily.Electronic,
            "hip hop" to GenreFamily.HipHop,
            "trap" to GenreFamily.HipHop,
            "bebop" to GenreFamily.Jazz,
            "baroque" to GenreFamily.Classical,
            "bluegrass" to GenreFamily.Folk,
            "nu metal" to GenreFamily.Metal,
            "post-punk" to GenreFamily.Punk,
            "neo soul" to GenreFamily.Soul,
            "reggaeton" to GenreFamily.Latin,
            "dub" to GenreFamily.Reggae,
            "delta blues" to GenreFamily.Blues,
            "dark ambient" to GenreFamily.Ambient
        ).forEach { (genre, family) -> assertEquals(genre, family, GenreFamily.of(genre)) }
    }

    @Test
    fun `the more specific family wins`() {
        assertEquals(GenreFamily.Punk, GenreFamily.of("punk rock"))
        assertEquals(GenreFamily.Rock, GenreFamily.of("pop rock"))
        assertEquals(GenreFamily.Electronic, GenreFamily.of("synth-pop"))
        assertEquals(GenreFamily.Blues, GenreFamily.of("blues rock"))
        assertEquals(GenreFamily.Soul, GenreFamily.of("rhythm and blues"))
        assertEquals(GenreFamily.Rock, GenreFamily.of("garage rock"))
        assertEquals(GenreFamily.Pop, GenreFamily.of("indie pop"))
        assertEquals(GenreFamily.HipHop, GenreFamily.of("hardcore hip hop"))
    }

    @Test
    fun `words match whole, not inside other words`() {
        // "trap" isn't in "trapeze", nor "rap" in "rapture".
        assertNull(GenreFamily.of("trapeze"))
        assertNull(GenreFamily.of("rapture"))
    }

    @Test
    fun `genres of the same family turn the palette, in order`() {
        val tiles = genreTiles(listOf("alternative rock", "art rock", "electronic", "experimental rock"))

        val rock = GenreFamily.Rock.palette
        assertEquals(rock, tiles[0].colors)
        assertEquals(rock.drop(1) + rock.take(1), tiles[1].colors)
        assertEquals(GenreFamily.Electronic.palette, tiles[2].colors)
        assertEquals(rock.drop(2) + rock.take(2), tiles[3].colors)
        assertEquals(listOf("alternative rock", "art rock", "electronic", "experimental rock"), tiles.map { it.genre })
    }

    @Test
    fun `a genre in no family gets its own palette, the same every time`() {
        val once = genreTiles(listOf("vaporwave")).single().colors

        assertEquals(once, genreTiles(listOf("Vaporwave")).single().colors)
        assertNotEquals(once, genreTiles(listOf("chiptune")).single().colors)
        assertEquals(5, once.size)
        assertTrue(once.all { it ushr 24 == 0xFFL })
    }

    @Test
    fun `hsl gives the expected colours`() {
        assertEquals(0xFFFF0000, hsl(0f, 1f, 0.5f))
        assertEquals(0xFF00FF00, hsl(120f, 1f, 0.5f))
        assertEquals(0xFFFFFFFF, hsl(0f, 0f, 1f))
    }

    @Test
    fun `no genres is no tiles`() {
        assertTrue(genreTiles(emptyList()).isEmpty())
    }
}
