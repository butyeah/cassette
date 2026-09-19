package com.ruidoespontaneo.cassette.itunes.data

import com.ruidoespontaneo.cassette.itunes.data.api.ItunesApi
import com.ruidoespontaneo.cassette.itunes.data.model.ItunesResponse
import com.ruidoespontaneo.cassette.itunes.data.model.ItunesResultDto
import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItunesRepositoryImplTest {

    private val album = AlbumDetail(
        id = "album-1",
        title = "In Between Dreams",
        artistName = "Jack Johnson",
        primaryType = "Album",
        firstReleaseDate = LocalDate.of(2005, 3, 1),
        genres = emptyList(),
        ratingValue = null,
        ratingVotesCount = 0
    )

    private fun song(name: String, number: Int, disc: Int = 1, preview: String? = "https://p/$name") =
        ItunesResultDto(
            wrapperType = "track", kind = "song", trackName = name,
            trackNumber = number, discNumber = disc, previewUrl = preview
        )

    private fun collection(id: Long, name: String, artist: String) = ItunesResultDto(
        wrapperType = "collection", collectionId = id, collectionName = name, artistName = artist
    )

    private class FakeItunesApi(
        val lookup: (Long, String?) -> ItunesResponse = { _, _ -> error("unexpected lookup") },
        val search: (String) -> ItunesResponse = { error("unexpected search") }
    ) : ItunesApi {
        override suspend fun lookupAlbumSongs(collectionId: Long, country: String?, entity: String) =
            lookup(collectionId, country)

        override suspend fun searchAlbums(term: String, entity: String, limit: Int) = search(term)
    }

    @Test
    fun `looks up by the Apple Music link's id and storefront, without searching`() = runBlocking {
        var lookedUp: Pair<Long, String?>? = null
        val api = FakeItunesApi(lookup = { id, country ->
            lookedUp = id to country
            ItunesResponse(listOf(collection(id, "In Between Dreams", "Jack Johnson"), song("Better Together", 1)))
        })
        val linked = album.copy(
            streamingLinks = StreamingLinks(appleMusic = "https://music.apple.com/gb/album/in-between-dreams/1440857781")
        )

        val result = ItunesRepositoryImpl(api).getPreviews(linked)

        assertEquals(1440857781L to "gb", lookedUp)
        assertEquals(listOf(TrackPreview("Better Together", "https://p/Better Together")), result.getOrNull())
    }

    @Test
    fun `orders songs by disc then track and drops rows without a preview`() = runBlocking {
        val api = FakeItunesApi(lookup = { _, _ ->
            ItunesResponse(
                listOf(
                    collection(1, "In Between Dreams", "Jack Johnson"),
                    song("D2T1", number = 1, disc = 2),
                    song("D1T2", number = 2, disc = 1),
                    song("NoPreview", number = 3, disc = 1, preview = null),
                    song("D1T1", number = 1, disc = 1)
                )
            )
        })
        val linked = album.copy(streamingLinks = StreamingLinks(appleMusic = "https://music.apple.com/us/album/x/1"))

        val result = ItunesRepositoryImpl(api).getPreviews(linked)

        assertEquals(listOf("D1T1", "D1T2", "D2T1"), result.getOrNull()?.map { it.title })
    }

    @Test
    fun `searches when there is no Apple Music link, taking the exact artist and title match`() = runBlocking {
        var lookedUpId: Long? = null
        var searchedTerm: String? = null
        val api = FakeItunesApi(
            search = { term ->
                searchedTerm = term
                ItunesResponse(
                    listOf(
                        collection(10, "In Between Dreams: Live", "Jack Johnson"),
                        collection(20, "In Between Dreams (Bonus Track Version)", "Jack Johnson")
                    )
                )
            },
            lookup = { id, _ ->
                lookedUpId = id
                ItunesResponse(listOf(song("Better Together", 1)))
            }
        )

        val result = ItunesRepositoryImpl(api).getPreviews(album)

        assertEquals("Jack Johnson In Between Dreams", searchedTerm)
        assertEquals(20L, lookedUpId)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `is an empty success, not a failure, when search finds no exact match`() = runBlocking {
        val api = FakeItunesApi(search = {
            ItunesResponse(listOf(collection(10, "Sleep Through the Static", "Jack Johnson")))
        })

        val result = ItunesRepositoryImpl(api).getPreviews(album)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `an unparseable Apple Music link falls back to search`() = runBlocking {
        var searched = false
        val api = FakeItunesApi(search = { searched = true; ItunesResponse() })
        val odd = album.copy(streamingLinks = StreamingLinks(appleMusic = "https://music.apple.com/us/artist/x/1"))

        ItunesRepositoryImpl(api).getPreviews(odd)

        assertTrue(searched)
    }

    @Test
    fun `returns a failure when the network call throws`() = runBlocking {
        val api = FakeItunesApi(search = { throw java.io.IOException("offline") })

        val result = ItunesRepositoryImpl(api).getPreviews(album)

        assertTrue(result.exceptionOrNull() is java.io.IOException)
        assertNull(result.getOrNull())
    }
}
