package com.ruidoespontaneo.cassette.facts.domain.model

import kotlinx.datetime.LocalDate

/**
 * One sentence about how an album came out, decided by
 * [com.ruidoespontaneo.cassette.facts.domain.usecase.GetReleaseStoryUseCase]. [phrase] says which
 * sentence to use; each platform words it from its own strings, filling in the rest.
 */
data class ReleaseStory(
    val phrase: Phrase,
    val date: LocalDate,
    val artist: String,
    /** Every label, in Wikidata's order. Empty for the phrases without one. */
    val labels: List<String>,
    /** Every producer, in Wikidata's order. Empty for the phrases without one. */
    val producers: List<String>
) {
    /** Which sentence fits: whether there's a label, and who produced the album. */
    enum class Phrase {
        /** A label, and one producer who isn't the artist. */
        LabelAndProducer,

        /** A label, and the artist produced it themself. */
        LabelSelfProduced,

        /** A label, and several producers (the artist may be one of them). */
        LabelAndProducers,

        /** A label, and no producer known. */
        LabelOnly,

        /** No label, and one producer who isn't the artist. */
        Producer,

        /** No label, and the artist produced it themself. */
        SelfProduced,

        /** No label, and several producers. */
        Producers,

        /** Neither: just the date. */
        DateOnly
    }
}
