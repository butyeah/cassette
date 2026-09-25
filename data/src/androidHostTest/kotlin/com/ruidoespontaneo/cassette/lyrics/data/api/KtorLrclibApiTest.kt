package com.ruidoespontaneo.cassette.lyrics.data.api

import com.ruidoespontaneo.cassette.core.network.lrclibHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorLrclibApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(body: String, status: HttpStatusCode = HttpStatusCode.OK): LrclibApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return KtorLrclibApi(lrclibHttpClient(engine, userAgent = "Cassette/1.0 (test)", logger = null))
    }

    @Test
    fun `gets one track by all four fields, sending the User-Agent`() = runBlocking {
        val api = api(
            """
                {"id": 13976, "trackName": "Karma Police", "artistName": "Radiohead", "albumName": "OK Computer",
                 "duration": 264.0, "instrumental": false, "plainLyrics": "Karma police\nArrest this man",
                 "syncedLyrics": "[00:26.78]Karma police"}
            """.trimIndent()
        )

        val track = api.get(artistName = "Radiohead", trackName = "Karma Police", albumName = "OK Computer", duration = 264)

        assertEquals(
            "https://lrclib.net/api/get?artist_name=Radiohead&track_name=Karma+Police&album_name=OK+Computer&duration=264",
            requests.single().url.toString()
        )
        assertEquals("Cassette/1.0 (test)", requests.single().headers[HttpHeaders.UserAgent])
        assertEquals("Karma police\nArrest this man", track.plainLyrics)
        assertEquals(264.0, track.duration)
    }

    @Test
    fun `searches by track and artist, parsing a list`() = runBlocking {
        val api = api("""[{"trackName": "Airbag", "albumName": "OK Computer", "instrumental": true}]""")

        val results = api.search(trackName = "Airbag", artistName = "Radiohead")

        assertEquals("/api/search", requests.single().url.encodedPath)
        assertEquals("Airbag", requests.single().url.parameters["track_name"])
        assertEquals("Radiohead", requests.single().url.parameters["artist_name"])
        assertTrue(results.single().instrumental)
    }

    @Test
    fun `a track LRCLIB doesn't know is a 404`() {
        val api = api("""{"code": 404, "name": "TrackNotFound"}""", HttpStatusCode.NotFound)

        val error = assertThrows(ClientRequestException::class.java) {
            runBlocking { api.get("A", "B", "C", 100) }
        }
        assertEquals(HttpStatusCode.NotFound, error.response.status)
    }
}
