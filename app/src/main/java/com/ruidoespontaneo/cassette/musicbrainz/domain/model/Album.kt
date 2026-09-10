package com.ruidoespontaneo.cassette.musicbrainz.domain.model

import java.time.LocalDate

/** Domain-level album (MusicBrainz release), decoupled from its wire format. */
data class Album(
    val id: String,
    val title: String,
    /**
     * The release date, when MusicBrainz has a precise day for it.
     * Releases with only a year or year-month can't be placed on a
     * calendar and come through as `null`.
     */
    val releaseDate: LocalDate?,
    val artistId: String?,
    val artistName: String
)
