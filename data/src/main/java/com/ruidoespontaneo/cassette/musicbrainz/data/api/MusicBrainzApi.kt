package com.ruidoespontaneo.cassette.musicbrainz.data.api

import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseSearchResponse
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
     * Full-text search over releases (albums).
     * https://musicbrainz.org/doc/MusicBrainz_API/Search#Release
     *
     * [query] is a raw Lucene query string, e.g.
     * `date:[2024-01-01 TO 2024-01-31] AND primarytype:album` to scope
     * results to a date range for a calendar view — built by
     * [com.ruidoespontaneo.cassette.musicbrainz.data.MusicBrainzRepositoryImpl].
     */
    @GET("release")
    suspend fun getAlbumsByDate(
        @Query("query") query: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): ReleaseSearchResponse
}
