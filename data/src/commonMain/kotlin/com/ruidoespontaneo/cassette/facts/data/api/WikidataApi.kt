package com.ruidoespontaneo.cassette.facts.data.api

/**
 * Wikidata's Action API (https://www.wikidata.org/w/api.php): free, no key, CC0 data.
 * Implemented by [KtorWikidataApi]; the base URL, `format=json` and User-Agent are configured once
 * in [com.ruidoespontaneo.cassette.core.network.wikidataHttpClient].
 */
interface WikidataApi {

    /**
     * The id ("Q202996") of the item whose MusicBrainz release group ID (P436) is
     * [musicBrainzReleaseGroupId], or `null` when there's none.
     */
    suspend fun findItem(musicBrainzReleaseGroupId: String): String?

    /**
     * [itemId]'s claims whose values are other items, as property id ("P162") to those items' ids,
     * in Wikidata's order. Deprecated claims and ones with no value are left out.
     */
    suspend fun itemClaims(itemId: String): Map<String, List<String>>

    /**
     * Each of [itemIds]' label in the first of [languages] it has one in, by item id. Items with a
     * label in none of them are absent. At most 50 ids per call, the API's limit.
     */
    suspend fun labels(itemIds: List<String>, languages: List<String>): Map<String, String>
}
