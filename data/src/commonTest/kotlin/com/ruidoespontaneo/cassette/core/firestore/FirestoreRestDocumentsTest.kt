package com.ruidoespontaneo.cassette.core.firestore

import com.ruidoespontaneo.cassette.core.network.firestoreHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreRestDocumentsTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun documents(status: HttpStatusCode = HttpStatusCode.OK, body: String): FirestoreRestDocuments {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json; charset=UTF-8"))
        }
        return FirestoreRestDocuments(firestoreHttpClient(engine, logger = null), projectId = "project")
    }

    @Test
    fun queryPostsEqualityFiltersAndParsesTypedValues() = runTest {
        val documents = documents(
            body = """
                [
                  {"document": {
                    "name": "projects/project/databases/(default)/documents/albumsByDay/rg-1",
                    "fields": {
                      "title": {"stringValue": "Calypso"},
                      "year": {"integerValue": "2002"},
                      "rating": {"doubleValue": 4.5},
                      "explicit": {"booleanValue": false},
                      "note": {"nullValue": null},
                      "genres": {"arrayValue": {}},
                      "tracks": {"arrayValue": {"values": [
                        {"mapValue": {"fields": {"position": {"integerValue": "1"}}}}
                      ]}},
                      "links": {"mapValue": {}}
                    }
                  }, "readTime": "2026-09-25T00:00:00Z"},
                  {"readTime": "2026-09-25T00:00:00Z"}
                ]
            """.trimIndent()
        )

        val result = documents.query("albumsByDay", fieldsEqualTo = mapOf("month" to 6, "day" to 17))

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/v1/projects/project/databases/(default)/documents:runQuery", request.url.encodedPath.decodeParens())
        val filters = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            .getValue("structuredQuery").jsonObject
            .getValue("where").jsonObject
            .getValue("compositeFilter").jsonObject
            .getValue("filters").jsonArray
            .map { it.jsonObject.getValue("fieldFilter").jsonObject }
        assertEquals(
            listOf("month" to "6", "day" to "17"),
            filters.map {
                it.getValue("field").jsonObject.getValue("fieldPath").jsonPrimitive.content to
                    it.getValue("value").jsonObject.getValue("integerValue").jsonPrimitive.content
            }
        )

        val document = result.single()
        assertEquals("rg-1", document.id)
        assertEquals(
            mapOf(
                "title" to "Calypso",
                "year" to 2002L,
                "rating" to 4.5,
                "explicit" to false,
                "note" to null,
                "genres" to emptyList<Any?>(),
                "tracks" to listOf(mapOf("position" to 1L)),
                "links" to emptyMap<String, Any?>()
            ),
            document.fields
        )
    }

    @Test
    fun getReadsOneDocumentByPath() = runTest {
        val documents = documents(
            body = """{"name": "projects/project/databases/(default)/documents/albumTracks/rg-1",
                       "fields": {"title": {"stringValue": "Discovery"}}}"""
        )

        val document = documents.get("albumTracks", "rg-1")

        assertEquals(
            "/v1/projects/project/databases/(default)/documents/albumTracks/rg-1",
            requests.single().url.encodedPath.decodeParens()
        )
        assertEquals("rg-1", document?.id)
        assertEquals(mapOf<String, Any?>("title" to "Discovery"), document?.fields)
    }

    @Test
    fun getReturnsNullForAMissingDocument() = runTest {
        val documents = documents(status = HttpStatusCode.NotFound, body = """{"error": {"code": 404}}""")

        assertNull(documents.get("albumTracks", "missing"))
    }

    // Ktor may percent-encode the parentheses in `(default)`; Firestore accepts either.
    private fun String.decodeParens() = replace("%28", "(").replace("%29", ")")
}
