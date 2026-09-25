package com.ruidoespontaneo.cassette.itunes.data.api

import com.ruidoespontaneo.cassette.core.network.itunesHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KtorItunesApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    // iTunes really does label its JSON this way.
    private fun api(body: String): ItunesApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/javascript; charset=utf-8"))
        }
        return KtorItunesApi(itunesHttpClient(engine, logger = null))
    }

    @Test
    fun `looks up an album's songs in a storefront, parsing text-javascript`() = runBlocking {
        val api = api(
            """
                {"resultCount": 2, "results": [
                  {"wrapperType": "collection", "collectionId": 1440857781, "collectionName": "In Between Dreams"},
                  {"wrapperType": "track", "kind": "song", "trackName": "Better Together", "trackNumber": 1,
                   "discNumber": 1, "previewUrl": "https://p/1", "trackPrice": 1.29}
                ]}
            """.trimIndent()
        )

        val results = api.lookupAlbumSongs(1440857781, country = "gb").results

        assertEquals(
            "https://itunes.apple.com/lookup?id=1440857781&country=gb&entity=song",
            requests.single().url.toString()
        )
        assertEquals(1440857781L, results[0].collectionId)
        assertEquals("https://p/1", results[1].previewUrl)
    }

    @Test
    fun `leaves out the country when there's none, and never adds fmt`() = runBlocking {
        val api = api("""{"resultCount": 0, "results": []}""")

        api.lookupAlbumSongs(1)
        api.searchAlbums("Jack Johnson In Between Dreams")

        val (lookup, search) = requests.map { it.url }
        assertFalse("country" in lookup.parameters.names())
        assertFalse(requests.any { "fmt" in it.url.parameters.names() })
        assertEquals("/search", search.encodedPath)
        assertEquals("Jack Johnson In Between Dreams", search.parameters["term"])
        assertEquals("album", search.parameters["entity"])
        assertEquals("5", search.parameters["limit"])
    }
}
