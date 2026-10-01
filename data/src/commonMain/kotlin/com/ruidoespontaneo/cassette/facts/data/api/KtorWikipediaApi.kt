package com.ruidoespontaneo.cassette.facts.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** [WikipediaApi] over a client from [com.ruidoespontaneo.cassette.core.network.wikipediaHttpClient]. */
class KtorWikipediaApi(private val client: HttpClient) : WikipediaApi {

    override suspend fun summary(language: String, title: String): WikipediaSummary? {
        // Each language is its own host, so there's no base URL to configure on the client.
        val url = URLBuilder("https://$language.wikipedia.org/api/rest_v1/page/summary").apply {
            // Wikipedia writes titles with underscores; encodeSlash keeps "AC/DC" one segment.
            appendPathSegments(listOf(title.replace(' ', '_')), encodeSlash = true)
        }.build()
        val response: SummaryDto = try {
            client.get(url).body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) return null
            throw e
        }
        if (response.type != "standard" || response.extract.isNullOrBlank()) return null
        val pageUrl = response.contentUrls?.desktop?.page ?: return null
        return WikipediaSummary(extract = response.extract.trim(), pageUrl = pageUrl)
    }
}

/** The parts of `GET /page/summary/{title}` the card uses. */
@Serializable
private data class SummaryDto(
    val type: String? = null,
    val extract: String? = null,
    @SerialName("content_urls") val contentUrls: ContentUrlsDto? = null
)

@Serializable
private data class ContentUrlsDto(val desktop: PageUrlDto? = null)

@Serializable
private data class PageUrlDto(val page: String? = null)
