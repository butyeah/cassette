package com.ruidoespontaneo.cassette.facts.domain

import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts

interface AlbumFactsRepository {

    /**
     * Facts about the album with MusicBrainz release-group id [releaseGroupId], named in [language]
     * (an ISO 639-1 code, like "es") where Wikidata has it, otherwise in English.
     *
     * @return [Result.success] with `null` when Wikidata has no item for the album, or nothing to say
     * about it (not an error: coverage is patchy). [Result.failure] only on a genuine
     * network/parsing failure.
     */
    suspend fun getAlbumFacts(releaseGroupId: String, language: String): Result<AlbumFacts?>
}
