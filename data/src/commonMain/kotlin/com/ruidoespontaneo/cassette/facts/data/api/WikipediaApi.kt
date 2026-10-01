package com.ruidoespontaneo.cassette.facts.data.api

/**
 * Wikipedia's REST API (https://en.wikipedia.org/api/rest_v1/): free, no key, CC BY-SA text.
 * Implemented by [KtorWikipediaApi]; the User-Agent is configured once in
 * [com.ruidoespontaneo.cassette.core.network.wikipediaHttpClient].
 */
interface WikipediaApi {

    /**
     * The lead of the article titled [title] on [language]'s Wikipedia ("en", "es"), or `null` when
     * there's no such article, it isn't a regular one (a disambiguation page, say), or its lead is
     * empty.
     */
    suspend fun summary(language: String, title: String): WikipediaSummary?
}

/** An article's lead paragraph as plain text, and the article's address. */
data class WikipediaSummary(val extract: String, val pageUrl: String)
