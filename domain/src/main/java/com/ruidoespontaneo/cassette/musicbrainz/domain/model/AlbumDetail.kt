package com.ruidoespontaneo.cassette.musicbrainz.domain.model

import java.time.LocalDate

/**
 * Full detail for one album (MusicBrainz release group), looked up by its
 * MBID — see [com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository.getAlbumDetail].
 * Richer than [Album], which only carries what a search result returns.
 */
data class AlbumDetail(
    val id: String,
    val title: String,
    val artistName: String,
    val primaryType: String?,
    /**
     * The release group's first release date, when MusicBrainz has a
     * precise day for it. A partial date ("2024" or "2024-01") can't be
     * placed on a calendar and comes through as `null`.
     */
    val firstReleaseDate: LocalDate?,
    val genres: List<String>,
    /** `null` when the album has no community rating yet. */
    val ratingValue: Double?,
    val ratingVotesCount: Int
)
