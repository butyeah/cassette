package com.ruidoespontaneo.cassette.musicbrainz.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response body of `GET /ws/2/artist?query=...`. */
@JsonClass(generateAdapter = true)
data class ArtistSearchResponse(
    val count: Int,
    val offset: Int,
    val artists: List<ArtistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArtistDto(
    val id: String,
    val name: String,
    @param:Json(name = "sort-name") val sortName: String? = null,
    val type: String? = null,
    val country: String? = null,
    val disambiguation: String? = null,
    // MusicBrainz's search API attaches a Lucene relevance score (0-100) to
    // each hit; it comes over the wire as a string.
    val score: String? = null
)
