package com.ruidoespontaneo.cassette.musicbrainz.data.api

import com.ruidoespontaneo.cassette.core.network.RateLimiter
import com.ruidoespontaneo.cassette.core.network.musicBrainzHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KtorMusicBrainzApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String): MusicBrainzApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = musicBrainzHttpClient(
            engine = engine,
            userAgent = "Cassette/1.0 (contact)",
            logger = null,
            rateLimiter = RateLimiter(minInterval = Duration.ZERO)
        )
        return KtorMusicBrainzApi(client)
    }

    @Test
    fun `looks up a release group as JSON, with the User-Agent`() = runBlocking {
        val api = api(
            body = """
                {
                  "id": "rg-1", "title": "Discovery", "primary-type": "Album",
                  "first-release-date": "2001-03-12",
                  "artist-credit": [{"name": "Daft Punk", "joinphrase": "", "artist": {"id": "a-1", "name": "Daft Punk"}}],
                  "genres": [{"name": "house", "count": 3}],
                  "rating": {"value": 4.5, "votes-count": 12},
                  "disambiguation": ""
                }
            """.trimIndent()
        )

        val detail = api.getReleaseGroup("rg-1")

        val request = requests.single()
        assertEquals(
            "https://musicbrainz.org/ws/2/release-group/rg-1?inc=artist-credits%2Bgenres%2Bratings&fmt=json",
            request.url.toString()
        )
        assertEquals("Cassette/1.0 (contact)", request.headers[HttpHeaders.UserAgent])
        assertEquals("Album", detail.primaryType)
        assertEquals("2001-03-12", detail.firstReleaseDate)
        assertEquals("Daft Punk", detail.artistCredit.single().name)
        assertEquals(listOf("house"), detail.genres.map { it.name })
        assertEquals(12, detail.rating?.votesCount)
    }

    @Test
    fun `searches releases with the query, paging and fmt=json`() = runBlocking {
        val api = api(body = """{"count": 0, "offset": 0, "releases": []}""")

        api.getAlbumsByDate(query = "date:[2001-03-12 TO 2001-03-12]", limit = 10, offset = 20)

        val url = requests.single().url
        assertEquals("/ws/2/release", url.encodedPath)
        assertEquals("date:[2001-03-12 TO 2001-03-12]", url.parameters["query"])
        assertEquals("10", url.parameters["limit"])
        assertEquals("20", url.parameters["offset"])
        assertEquals(listOf("json"), url.parameters.getAll("fmt"))
    }

    @Test
    fun `browses a release group's official releases with recordings`() = runBlocking {
        val api = api(
            body = """
                {"releases": [{"id": "r-1", "media": [{"tracks": [
                  {"position": 1, "title": "One More Time", "length": 320357},
                  {"position": 2, "title": "Aerodynamic", "length": null}
                ]}]}]}
            """.trimIndent()
        )

        val releases = api.getReleasesForReleaseGroup("rg-1").releases

        val url = requests.single().url
        assertEquals("/ws/2/release", url.encodedPath)
        assertEquals("rg-1", url.parameters["release-group"])
        assertEquals("recordings", url.parameters["inc"])
        assertEquals("official", url.parameters["status"])
        assertEquals(listOf(320357, null), releases.single().media.single().tracks.map { it.length })
    }

    @Test
    fun `an error status throws instead of parsing the body`() {
        val api = api(status = HttpStatusCode.ServiceUnavailable, body = "slow down")

        val error = assertThrows(ServerResponseException::class.java) { runBlocking { api.getReleaseGroup("rg-1") } }

        assertEquals(HttpStatusCode.ServiceUnavailable, error.response.status)
    }
}
