package com.ruidoespontaneo.cassette.musicbrainz.domain.model

/** Domain-level artist, decoupled from MusicBrainz's wire format. */
data class Artist(
    val id: String,
    val name: String,
    val sortName: String?,
    val type: String?,
    val country: String?,
    val disambiguation: String?,
    val score: Int?
)
