package com.ruidoespontaneo.cassette.musicbrainz.data.api

import com.ruidoespontaneo.cassette.musicbrainz.data.model.ArtistSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * MusicBrainz web service v2 (https://musicbrainz.org/doc/MusicBrainz_API).
 * The base URL and JSON format (`fmt=json`) are configured once in
 * [com.ruidoespontaneo.cassette.core.network.di.NetworkModule], so endpoints
 * here only need their path and query parameters.
 */
interface MusicBrainzApi {

    /**
     * Full-text search over artists.
     * https://musicbrainz.org/doc/MusicBrainz_API/Search#Artist
     */
    @GET("artist")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0
    ): ArtistSearchResponse
}
