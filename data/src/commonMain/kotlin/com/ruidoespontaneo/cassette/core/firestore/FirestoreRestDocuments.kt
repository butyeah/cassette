package com.ruidoespontaneo.cassette.core.firestore

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * [FirestoreDocuments] over Firestore's REST API (https://firebase.google.com/docs/firestore/use-rest-api),
 * with no sign-in: the collections it reads are public (see firestore.rules). This is what iOS
 * uses, so the app doesn't need the Firebase iOS SDK until it needs auth.
 *
 * [client] comes from [com.ruidoespontaneo.cassette.core.network.firestoreHttpClient].
 */
class FirestoreRestDocuments(
    private val client: HttpClient,
    projectId: String
) : FirestoreDocuments {

    private val documentsUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    override suspend fun query(collection: String, fieldsEqualTo: Map<String, Int>): List<FirestoreDocument> {
        val body = buildJsonObject {
            putJsonObject("structuredQuery") {
                putJsonArray("from") { addJsonObject { put("collectionId", collection) } }
                putJsonObject("where") {
                    putJsonObject("compositeFilter") {
                        put("op", "AND")
                        putJsonArray("filters") {
                            fieldsEqualTo.forEach { (field, value) ->
                                addJsonObject {
                                    putJsonObject("fieldFilter") {
                                        putJsonObject("field") { put("fieldPath", field) }
                                        put("op", "EQUAL")
                                        // Firestore's REST API sends 64-bit integers as strings.
                                        putJsonObject("value") { put("integerValue", value.toString()) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // One row per result, plus rows with only a `readTime` (no `document`) to skip.
        val rows: JsonArray = client.post("$documentsUrl:runQuery") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
        return rows.mapNotNull { row -> row.jsonObject["document"]?.jsonObject?.toDocument() }
    }

    override suspend fun get(collection: String, id: String): FirestoreDocument? {
        val response: HttpResponse = client.get(documentsUrl) {
            url { appendPathSegments(collection, id) }
            // A missing document is an answer ("not cached"), not a failure.
            expectSuccess = false
        }
        return when {
            response.status == HttpStatusCode.NotFound -> null
            response.status.value !in 200..299 -> error("Firestore GET $collection/$id failed: ${response.status}")
            else -> response.body<JsonObject>().toDocument()
        }
    }
}

/** `{"name": ".../documents/<collection>/<id>", "fields": {...}}` → [FirestoreDocument]. */
internal fun JsonObject.toDocument(): FirestoreDocument {
    val id = getValue("name").jsonPrimitive.content.substringAfterLast('/')
    return FirestoreDocument(id, this["fields"]?.jsonObject?.toFields().orEmpty())
}

private fun JsonObject.toFields(): Map<String, Any?> = mapValues { (_, value) -> value.jsonObject.toValue() }

/**
 * One typed REST value (`{"stringValue": "…"}`, `{"integerValue": "6"}`, …) → the plain value the
 * Android SDK would have returned for it. Empty arrays and maps come back with no `values` /
 * `fields` key at all.
 */
private fun JsonObject.toValue(): Any? {
    val (type, raw) = entries.single()
    return when (type) {
        "stringValue" -> raw.jsonPrimitive.content
        "integerValue" -> raw.jsonPrimitive.content.toLong()
        "doubleValue" -> raw.jsonPrimitive.double
        "booleanValue" -> raw.jsonPrimitive.boolean
        "nullValue" -> null
        "arrayValue" -> raw.jsonObject["values"]?.jsonArray?.map { it.jsonObject.toValue() }.orEmpty()
        "mapValue" -> raw.jsonObject["fields"]?.jsonObject?.toFields().orEmpty()
        // Timestamps, references, bytes and geo points: none in these collections.
        else -> raw.toString()
    }
}
