package com.ruidoespontaneo.cassette.musicbrainz.data.api

import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseBrowseResponse
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseGroupDetailDto
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseSearchResponse

/**
 * MusicBrainz web service v2 (https://musicbrainz.org/doc/MusicBrainz_API),
 * implemented by [KtorMusicBrainzApi]. The base URL, JSON format (`fmt=json`),
 * User-Agent and rate limit are configured once in
 * [com.ruidoespontaneo.cassette.core.network.musicBrainzHttpClient], so endpoints
 * only need their path and query parameters.
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
    suspend fun getAlbumsByDate(
        query: String,
        limit: Int = 100,
        offset: Int = 0
    ): ReleaseSearchResponse

    /**
     * Look up one release group (album) by its MusicBrainz id.
     * https://musicbrainz.org/doc/MusicBrainz_API#Lookups
     *
     * [id] is the release-group MBID, e.g. [com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album.id].
     */
    suspend fun getReleaseGroup(
        id: String,
        inc: String = "artist-credits+genres+ratings"
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
    suspend fun getReleasesForReleaseGroup(
        releaseGroupId: String,
        inc: String = "recordings",
        status: String = "official"
    ): ReleaseBrowseResponse
}
