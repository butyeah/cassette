package com.ruidoespontaneo.cassette.facts.data.api

import com.ruidoespontaneo.cassette.core.network.wikidataHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorWikidataApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(body: String): WikidataApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8"))
        }
        return KtorWikidataApi(wikidataHttpClient(engine, userAgent = "Cassette/1.0 (test)", logger = null))
    }

    @Test
    fun `finds the item by its MusicBrainz release group id, as JSON with the User-Agent`() = runBlocking {
        val api = api("""{"batchcomplete": "", "query": {"search": [{"ns": 0, "title": "Q202996", "pageid": 199506}]}}""")

        assertEquals("Q202996", api.findItem("b1392450-e666-3926-a536-22c65f834433"))

        val url = requests.single().url
        assertEquals("/w/api.php", url.encodedPath)
        assertEquals("json", url.parameters["format"])
        assertEquals("search", url.parameters["list"])
        assertEquals("haswbstatement:P436=b1392450-e666-3926-a536-22c65f834433", url.parameters["srsearch"])
        assertEquals("Cassette/1.0 (test)", requests.single().headers[HttpHeaders.UserAgent])
    }

    @Test
    fun `no search hit is no item`() = runBlocking {
        assertNull(api("""{"batchcomplete": "", "query": {"search": []}}""").findItem("unknown"))
    }

    @Test
    fun `reads item-valued claims, skipping deprecated, valueless and non-item ones, and article titles`() = runBlocking {
        val api = api(
            """
                {"entities": {"Q202996": {"type": "item", "id": "Q202996", "claims": {
                  "P162": [{"mainsnak": {"snaktype": "value", "property": "P162",
                            "datavalue": {"value": {"entity-type": "item", "numeric-id": 544301, "id": "Q544301"}, "type": "wikibase-entityid"}},
                            "rank": "normal"}],
                  "P264": [{"mainsnak": {"datavalue": {"value": {"id": "Q208909"}}}, "rank": "preferred"},
                           {"mainsnak": {"datavalue": {"value": {"id": "Q1"}}}, "rank": "deprecated"},
                           {"mainsnak": {"snaktype": "somevalue"}, "rank": "normal"}],
                  "P444": [{"mainsnak": {"datavalue": {"value": "94", "type": "string"}}, "rank": "normal"}]
                }, "sitelinks": {"enwiki": {"site": "enwiki", "title": "OK Computer", "badges": []}}}}}
            """.trimIndent()
        )

        val item = api.item("Q202996", languages = listOf("es", "en"))

        assertEquals(mapOf("P162" to listOf("Q544301"), "P264" to listOf("Q208909")), item.claims)
        assertEquals(mapOf("en" to "OK Computer"), item.articleTitles)
        val url = requests.single().url
        assertEquals("wbgetentities", url.parameters["action"])
        assertEquals("claims|sitelinks", url.parameters["props"])
        assertEquals("eswiki|enwiki", url.parameters["sitefilter"])
    }

    @Test
    fun `an entity with no claims or sitelinks, written as empty arrays, has none`() = runBlocking {
        val item = api("""{"entities": {"Q1": {"id": "Q1", "claims": [], "sitelinks": []}}}""").item("Q1", listOf("en"))

        assertEquals(WikidataItem(), item)
    }

    @Test
    fun `labels come in the first language each item has, in one call`() = runBlocking {
        val api = api(
            """
                {"entities": {
                  "Q544301": {"id": "Q544301", "labels": {"en": {"language": "en", "value": "Nigel Godrich"}}},
                  "Q208909": {"id": "Q208909", "labels": {"es": {"language": "es", "value": "Parlophone (es)"},
                                                          "en": {"language": "en", "value": "Parlophone"}}},
                  "Q9": {"id": "Q9", "labels": []}
                }}
            """.trimIndent()
        )

        val labels = api.labels(listOf("Q544301", "Q208909", "Q9"), languages = listOf("es", "en"))

        assertEquals(mapOf("Q544301" to "Nigel Godrich", "Q208909" to "Parlophone (es)"), labels)
        val url = requests.single().url
        assertEquals("Q544301|Q208909|Q9", url.parameters["ids"])
        assertEquals("es|en", url.parameters["languages"])
        assertEquals("1", url.parameters["languagefallback"])
    }

    @Test
    fun `no ids need no call`() = runBlocking {
        assertTrue(api("{}").labels(emptyList(), listOf("en")).isEmpty())
        assertTrue(requests.isEmpty())
    }
}
