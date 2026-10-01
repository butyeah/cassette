package com.ruidoespontaneo.cassette.facts.domain.model

/**
 * What Wikidata knows about an album, as names in the user's language where it has them. Each list
 * is empty when Wikidata says nothing about it.
 */
data class AlbumFacts(
    val producers: List<String> = emptyList(),
    val recordedAt: List<String> = emptyList(),
    val coverArtBy: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val awards: List<String> = emptyList(),
    val nominations: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = listOf(producers, recordedAt, coverArtBy, labels, awards, nominations).all { it.isEmpty() }
}
