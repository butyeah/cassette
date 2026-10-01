package com.ruidoespontaneo.cassette.facts.domain.model

/**
 * What Wikidata knows about an album, as names in the user's language where it has them. Each list
 * is empty when Wikidata says nothing about it. [summary] is the lead of the album's Wikipedia
 * article, when it has one.
 */
data class AlbumFacts(
    val producers: List<String> = emptyList(),
    val recordedAt: List<String> = emptyList(),
    val coverArtBy: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val awards: List<String> = emptyList(),
    val nominations: List<String> = emptyList(),
    val summary: AlbumSummary? = null
) {
    val isEmpty: Boolean
        get() = summary == null &&
            listOf(producers, recordedAt, coverArtBy, labels, awards, nominations).all { it.isEmpty() }
}

/**
 * The lead paragraph of an album's Wikipedia article, and the article itself. Wikipedia's text is
 * CC BY-SA, so wherever [text] is shown, Wikipedia is credited and [articleUrl] linked.
 */
data class AlbumSummary(val text: String, val articleUrl: String)
