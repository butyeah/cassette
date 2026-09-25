package com.ruidoespontaneo.cassette.facts.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * [WikidataApi] over a client from [com.ruidoespontaneo.cassette.core.network.wikidataHttpClient].
 *
 * Responses are read as plain JSON rather than DTOs: the API writes an empty map as `[]` (PHP's
 * empty array), so an entity with no claims or labels would fail a typed decode.
 */
class KtorWikidataApi(private val client: HttpClient) : WikidataApi {

    override suspend fun findItem(musicBrainzReleaseGroupId: String): String? {
        val response: JsonObject = client.get("api.php") {
            parameter("action", "query")
            parameter("list", "search")
            parameter("srsearch", "haswbstatement:P436=$musicBrainzReleaseGroupId")
            parameter("srlimit", 1)
        }.body()
        val hits = (response.obj("query")?.get("search") as? JsonArray).orEmpty()
        return (hits.firstOrNull() as? JsonObject)?.string("title")
    }

    override suspend fun itemClaims(itemId: String): Map<String, List<String>> {
        val entity = entities {
            parameter("ids", itemId)
            parameter("props", "claims")
        }[itemId] as? JsonObject ?: return emptyMap()
        val claims = entity["claims"] as? JsonObject ?: return emptyMap()
        return claims.mapValues { (_, statements) ->
            (statements as? JsonArray).orEmpty().mapNotNull { statement ->
                val claim = statement as? JsonObject ?: return@mapNotNull null
                if (claim.string("rank") == "deprecated") return@mapNotNull null
                claim.obj("mainsnak")?.obj("datavalue")?.obj("value")?.string("id")
            }
        }.filterValues { it.isNotEmpty() }
    }

    override suspend fun labels(itemIds: List<String>, languages: List<String>): Map<String, String> {
        if (itemIds.isEmpty()) return emptyMap()
        val entities = entities {
            parameter("ids", itemIds.joinToString("|"))
            parameter("props", "labels")
            parameter("languages", languages.joinToString("|"))
            // Also fills a missing language from its fallbacks (a regional variant, say).
            parameter("languagefallback", 1)
        }
        return itemIds.mapNotNull { id ->
            val labels = (entities[id] as? JsonObject)?.get("labels") as? JsonObject ?: return@mapNotNull null
            val label = languages.firstNotNullOfOrNull { labels.obj(it)?.string("value") } ?: return@mapNotNull null
            id to label
        }.toMap()
    }

    private suspend fun entities(block: HttpRequestBuilder.() -> Unit): JsonObject {
        val response: JsonObject = client.get("api.php") {
            parameter("action", "wbgetentities")
            block()
        }.body()
        return response.obj("entities") ?: JsonObject(emptyMap())
    }
}

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content
