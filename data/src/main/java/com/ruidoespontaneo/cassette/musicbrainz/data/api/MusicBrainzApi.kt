package com.ruidoespontaneo.cassette.musicbrainz.data.api

import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseBrowseResponse
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseGroupDetailDto
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
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

    /**
     * Look up one release group (album) by its MusicBrainz id.
     * https://musicbrainz.org/doc/MusicBrainz_API#Lookups
     *
     * [id] is the release-group MBID, e.g. [com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album.id].
     */
    @GET("release-group/{id}")
    suspend fun getReleaseGroup(
        @Path("id") id: String,
        @Query("inc") inc: String = "artist-credits+genres+ratings"
    ): ReleaseGroupDetailDto

    /**
     * Releases belonging to release group [releaseGroupId], each with its tracklist.
     * https://musicbrainz.org/doc/MusicBrainz_API#Browse
     *
     * A release group has no tracks of its own — different releases of the same group
     * (reissues, remasters, regional editions) can have different tracklists — so
     * [com.ruidoespontaneo.cassette.musicbrainz.data.MusicBrainzRepositoryImpl] picks one
     * of these to represent the group's tracklist on [com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail].
     */
    @GET("release")
    suspend fun getReleasesForReleaseGroup(
        @Query("release-group") releaseGroupId: String,
        @Query("inc") inc: String = "recordings",
        @Query("status") status: String = "official"
    ): ReleaseBrowseResponse
}
