package com.ruidoespontaneo.cassette.facts.data

import com.ruidoespontaneo.cassette.facts.data.api.WikidataApi
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WikidataAlbumFactsRepositoryTest {

    private val calls = mutableListOf<String>()

    private inner class FakeWikidataApi(
        val item: String? = "Q1",
        val claims: Map<String, List<String>> = emptyMap(),
        val names: Map<String, String> = emptyMap(),
        val failWith: Exception? = null
    ) : WikidataApi {
        override suspend fun findItem(musicBrainzReleaseGroupId: String): String? {
            calls += "find $musicBrainzReleaseGroupId"
            failWith?.let { throw it }
            return item
        }

        override suspend fun itemClaims(itemId: String): Map<String, List<String>> {
            calls += "claims $itemId"
            return claims
        }

        override suspend fun labels(itemIds: List<String>, languages: List<String>): Map<String, String> {
            calls += "labels ${itemIds.joinToString(",")} in ${languages.joinToString(",")}"
            return names.filterKeys { it in itemIds }
        }
    }

    @Test
    fun `each property fills its own row, named in one call`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(
            FakeWikidataApi(
                claims = mapOf(
                    "P162" to listOf("Q10"),
                    "P483" to listOf("Q11"),
                    "P736" to listOf("Q12"),
                    "P264" to listOf("Q13", "Q14"),
                    "P166" to listOf("Q15"),
                    "P1411" to listOf("Q16"),
                    "P444" to listOf("Q99") // not shown on the card
                ),
                names = mapOf(
                    "Q10" to "Nigel Godrich", "Q11" to "St Catherine's Court", "Q12" to "Stanley Donwood",
                    "Q13" to "Parlophone", "Q14" to "Capitol", "Q15" to "Grammy", "Q16" to "Brit Award", "Q99" to "Metacritic"
                )
            )
        )

        val facts = repository.getAlbumFacts("mbid", "es").getOrThrow()

        assertEquals(
            AlbumFacts(
                producers = listOf("Nigel Godrich"),
                recordedAt = listOf("St Catherine's Court"),
                coverArtBy = listOf("Stanley Donwood"),
                labels = listOf("Parlophone", "Capitol"),
                awards = listOf("Grammy"),
                nominations = listOf("Brit Award")
            ),
            facts
        )
        assertEquals(listOf("find mbid", "claims Q1", "labels Q10,Q11,Q12,Q13,Q14,Q15,Q16 in es,en"), calls)
    }

    @Test
    fun `English asks for English only, and an item with no label is dropped`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(
            FakeWikidataApi(claims = mapOf("P162" to listOf("Q10", "Q11")), names = mapOf("Q10" to "Nigel Godrich"))
        )

        assertEquals(listOf("Nigel Godrich"), repository.getAlbumFacts("mbid", "en").getOrThrow()?.producers)
        assertEquals("labels Q10,Q11 in en", calls.last())
    }

    @Test
    fun `no item is null, without asking further`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(FakeWikidataApi(item = null))

        assertNull(repository.getAlbumFacts("mbid", "en").getOrThrow())
        assertEquals(listOf("find mbid"), calls)
    }

    @Test
    fun `an item with none of the card's facts is null`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(FakeWikidataApi(claims = mapOf("P444" to listOf("Q99"))))

        assertNull(repository.getAlbumFacts("mbid", "en").getOrThrow())
    }

    @Test
    fun `facts whose items all lack labels are null`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(FakeWikidataApi(claims = mapOf("P162" to listOf("Q10"))))

        assertNull(repository.getAlbumFacts("mbid", "en").getOrThrow())
    }

    @Test
    fun `a network error is a failure`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(FakeWikidataApi(failWith = IOException("offline")))

        assertTrue(repository.getAlbumFacts("mbid", "en").isFailure)
    }

    @Test
    fun `a repeat lookup comes from the cache, per language`() = runBlocking {
        val repository = WikidataAlbumFactsRepository(FakeWikidataApi(item = null))

        repository.getAlbumFacts("mbid", "en")
        repository.getAlbumFacts("mbid", "en")
        repository.getAlbumFacts("mbid", "es")

        assertEquals(listOf("find mbid", "find mbid"), calls)
    }
}
