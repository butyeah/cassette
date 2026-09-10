package com.ruidoespontaneo.cassette.dayinhistory.domain.model

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album

/** Every album released on a given day/month in [year] — one card's worth, on the "one day like today" screen. */
data class AlbumsByYear(
    val year: Int,
    val albums: List<Album>
)
