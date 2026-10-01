package com.ruidoespontaneo.cassette.facts.data.api

import com.ruidoespontaneo.cassette.core.network.wikipediaHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KtorWikipediaApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(body: String, status: HttpStatusCode = HttpStatusCode.OK): WikipediaApi {
        val engine = MockEngine { request ->
            requests += request
            // Wikipedia's actual content type, profile and all.
            respond(
                body,
                status,
                headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8; profile=\"https://www.mediawiki.org/wiki/Specs/Summary/1.4.2\"")
            )
        }
        return KtorWikipediaApi(wikipediaHttpClient(engine, userAgent = "Cassette/1.0 (test)", logger = null))
    }

    @Test
    fun `reads the lead from the language's own Wikipedia, with the User-Agent`() = runBlocking {
        val api = api(
            """
                {"type": "standard", "title": "OK Computer",
                 "extract": "OK Computer is the third studio album by Radiohead. ",
                 "content_urls": {"desktop": {"page": "https://es.wikipedia.org/wiki/OK_Computer"}}}
            """.trimIndent()
        )

        val summary = api.summary("es", "OK Computer")

        assertEquals(WikipediaSummary("OK Computer is the third studio album by Radiohead.", "https://es.wikipedia.org/wiki/OK_Computer"), summary)
        val request = requests.single()
        assertEquals("es.wikipedia.org", request.url.host)
        assertEquals("/api/rest_v1/page/summary/OK_Computer", request.url.encodedPath)
        assertEquals("Cassette/1.0 (test)", request.headers[HttpHeaders.UserAgent])
    }

    @Test
    fun `a slash in the title stays in one path segment`() = runBlocking {
        api("""{"type": "standard", "extract": "x", "content_urls": {"desktop": {"page": "p"}}}""").summary("en", "Back in Black (AC/DC album)")

        assertEquals("/api/rest_v1/page/summary/Back_in_Black_(AC%2FDC_album)", requests.single().url.encodedPath)
    }

    @Test
    fun `a disambiguation page is no summary`() = runBlocking {
        assertNull(
            api("""{"type": "disambiguation", "extract": "OK may refer to:", "content_urls": {"desktop": {"page": "p"}}}""")
                .summary("en", "OK")
        )
    }

    @Test
    fun `a missing article is no summary, not an error`() = runBlocking {
        assertNull(api("""{"type": "https://mediawiki.org/wiki/HyperSwitch/errors/not_found"}""", HttpStatusCode.NotFound).summary("en", "Nope"))
    }

    @Test
    fun `an empty lead is no summary`() = runBlocking {
        assertNull(api("""{"type": "standard", "extract": " ", "content_urls": {"desktop": {"page": "p"}}}""").summary("en", "Blank"))
    }
}
