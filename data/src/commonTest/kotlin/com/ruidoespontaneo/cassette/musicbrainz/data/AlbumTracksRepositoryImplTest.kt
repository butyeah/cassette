package com.ruidoespontaneo.cassette.musicbrainz.data

import com.ruidoespontaneo.cassette.core.firestore.FirestoreDocument
import com.ruidoespontaneo.cassette.core.firestore.FirestoreDocuments
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class AlbumTracksRepositoryImplTest {

    private class FakeFirestore(private val document: FirestoreDocument?) : FirestoreDocuments {
        override suspend fun query(collection: String, fieldsEqualTo: Map<String, Int>) = error("unexpected query")
        override suspend fun get(collection: String, id: String) = document?.takeIf { collection == "albumTracks" && it.id == id }
    }

    private val fields = mapOf<String, Any?>(
        "title" to "Discovery",
        "artistName" to "Daft Punk",
        "year" to 2001L,
        "month" to 3L,
        "day" to 12L,
        "genres" to listOf("house", "french house"),
        "tracks" to listOf(
            mapOf("position" to 1L, "title" to "One More Time", "lengthMs" to 320357L),
            mapOf("position" to 2L, "title" to "Aerodynamic"),
            mapOf("title" to "No position, so skipped")
        ),
        "streamingLinks" to mapOf("appleMusic" to "https://music.apple.com/us/album/1")
    )

    @Test
    fun parsesACachedAlbum() = runTest {
        val repository = AlbumTracksRepositoryImpl(FakeFirestore(FirestoreDocument("rg-1", fields)))

        val detail = repository.getCachedAlbumDetail("rg-1").getOrThrow()!!

        assertEquals("Discovery", detail.title)
        assertEquals("Daft Punk", detail.artistName)
        assertEquals("Album", detail.primaryType)
        assertEquals(LocalDate(2001, 3, 12), detail.firstReleaseDate)
        assertEquals(listOf("house", "french house"), detail.genres)
        assertNull(detail.ratingValue)
        assertEquals(
            listOf(Track(1, "One More Time", 320357), Track(2, "Aerodynamic", null)),
            detail.tracks
        )
        assertEquals(StreamingLinks(appleMusic = "https://music.apple.com/us/album/1"), detail.streamingLinks)
    }

    @Test
    fun aMissingDocumentIsNotCached() = runTest {
        val repository = AlbumTracksRepositoryImpl(FakeFirestore(null))

        assertNull(repository.getCachedAlbumDetail("rg-1").getOrThrow())
    }

    @Test
    fun aDocumentMissingARequiredFieldIsTreatedAsNotCached() = runTest {
        val repository = AlbumTracksRepositoryImpl(FakeFirestore(FirestoreDocument("rg-1", fields - "artistName")))

        assertNull(repository.getCachedAlbumDetail("rg-1").getOrThrow())
    }

    @Test
    fun emptyListsAndNoLinksParseAsEmpty() = runTest {
        val sparse = fields + mapOf("genres" to emptyList<Any?>(), "tracks" to emptyList<Any?>()) - "streamingLinks"
        val repository = AlbumTracksRepositoryImpl(FakeFirestore(FirestoreDocument("rg-1", sparse)))

        val detail = repository.getCachedAlbumDetail("rg-1").getOrThrow()!!

        assertTrue(detail.genres.isEmpty())
        assertTrue(detail.tracks.isEmpty())
        assertEquals(StreamingLinks(), detail.streamingLinks)
    }
}
