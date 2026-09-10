package com.ruidoespontaneo.cassette.musicbrainz.domain

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Artist

interface MusicBrainzRepository {

    /**
     * Full-text search over artists.
     *
     * @return [Result.success] with the matches (possibly empty), or
     * [Result.failure] with the underlying exception on a network or HTTP
     * error — callers decide how to surface that to the UI.
     */
    suspend fun searchArtists(
        query: String,
        limit: Int = 25,
        offset: Int = 0
    ): Result<List<Artist>>
}
