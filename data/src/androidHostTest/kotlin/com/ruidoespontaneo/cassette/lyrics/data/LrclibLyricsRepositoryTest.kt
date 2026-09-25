package com.ruidoespontaneo.cassette.lyrics.data

import com.ruidoespontaneo.cassette.lyrics.data.api.LrclibApi
import com.ruidoespontaneo.cassette.lyrics.data.model.LrclibTrack
import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LrclibLyricsRepositoryTest {

    private val calls = mutableListOf<String>()

    private inner class FakeLrclibApi(
        val get: () -> LrclibTrack = { error("unexpected get") },
        val search: () -> List<LrclibTrack> = { error("unexpected search") }
    ) : LrclibApi {
        override suspend fun get(artistName: String, trackName: String, albumName: String, duration: Int): LrclibTrack {
            calls += "get $duration"
            return get()
        }

        override suspend fun search(trackName: String, artistName: String): List<LrclibTrack> {
            calls += "search"
            return search()
        }
    }

    private fun lyrics(album: String?, text: String, duration: Double? = 200.0) =
        LrclibTrack(trackName = "Song", artistName = "Artist", albumName = album, duration = duration, plainLyrics = text)

    /** What Ktor throws for a 404, since the exception needs a real response to carry. */
    private fun notFound(): Nothing = runBlocking {
        HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }) { expectSuccess = true }
            .get("https://lrclib.net/api/get")
        error("expected a 404")
    }

    private suspend fun LrclibLyricsRepository.lookup(durationSeconds: Int? = 200) =
        getLyrics(artist = "Artist", track = "Song", album = "Album", durationSeconds = durationSeconds)

    @Test
    fun `an exact match by duration is used without searching`() = runBlocking {
        val repository = LrclibLyricsRepository(FakeLrclibApi(get = { lyrics("Album", "  words\n") }))

        assertEquals(Lyrics.Plain("words"), repository.lookup().getOrThrow())
        assertEquals(listOf("get 200"), calls)
    }

    @Test
    fun `a 404 falls back to searching`() = runBlocking {
        val repository = LrclibLyricsRepository(
            FakeLrclibApi(get = { notFound() }, search = { listOf(lyrics("Album", "found by search")) })
        )

        assertEquals(Lyrics.Plain("found by search"), repository.lookup().getOrThrow())
        assertEquals(listOf("get 200", "search"), calls)
    }

    @Test
    fun `with no duration it searches straight away`() = runBlocking {
        val repository = LrclibLyricsRepository(FakeLrclibApi(search = { listOf(lyrics("Album", "words")) }))

        repository.lookup(durationSeconds = null)

        assertEquals(listOf("search"), calls)
    }

    @Test
    fun `search prefers the same album, then the closest length, and skips empty results`() = runBlocking {
        val repository = LrclibLyricsRepository(
            FakeLrclibApi(
                get = { notFound() },
                search = {
                    listOf(
                        LrclibTrack(albumName = "Album", duration = 200.0, plainLyrics = "  "),
                        lyrics("Live at Somewhere", "live", duration = 200.0),
                        lyrics("Album (Deluxe Edition)", "far", duration = 260.0),
                        lyrics("ALBUM", "close", duration = 201.0)
                    )
                }
            )
        )

        assertEquals(Lyrics.Plain("close"), repository.lookup().getOrThrow())
    }

    @Test
    fun `an instrumental track is Instrumental`() = runBlocking {
        val repository = LrclibLyricsRepository(FakeLrclibApi(get = { LrclibTrack(instrumental = true) }))

        assertEquals(Lyrics.Instrumental, repository.lookup().getOrThrow())
    }

    @Test
    fun `nothing anywhere is null, not a failure`() = runBlocking {
        val repository = LrclibLyricsRepository(FakeLrclibApi(get = { notFound() }, search = { emptyList() }))

        val result = repository.lookup()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `a network error is a failure, and isn't remembered`() = runBlocking {
        var fail = true
        val repository = LrclibLyricsRepository(
            FakeLrclibApi(get = { if (fail) throw IOException("offline") else lyrics("Album", "words") })
        )

        assertTrue(repository.lookup().isFailure)
        fail = false
        assertEquals(Lyrics.Plain("words"), repository.lookup().getOrThrow())
    }

    @Test
    fun `a second lookup of the same track comes from the cache, found-nothing included`() = runBlocking {
        val repository = LrclibLyricsRepository(FakeLrclibApi(get = { notFound() }, search = { emptyList() }))

        repository.lookup()
        repository.lookup()

        assertEquals(listOf("get 200", "search"), calls)
    }
}
