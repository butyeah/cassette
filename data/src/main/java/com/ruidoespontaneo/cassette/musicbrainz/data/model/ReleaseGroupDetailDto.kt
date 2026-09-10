package com.ruidoespontaneo.cassette.musicbrainz.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response body of `GET /ws/2/release-group/{id}?inc=artist-credits+genres+ratings`. */
@JsonClass(generateAdapter = true)
data class ReleaseGroupDetailDto(
    val id: String,
    val title: String,
    @param:Json(name = "primary-type") val primaryType: String? = null,
    // ISO 8601, but MusicBrainz allows partial dates ("2024" or "2024-01")
    // when a release group's first release's day or month isn't known.
    @param:Json(name = "first-release-date") val firstReleaseDate: String? = null,
    @param:Json(name = "artist-credit") val artistCredit: List<ArtistCreditDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val rating: RatingDto? = null
)

@JsonClass(generateAdapter = true)
data class GenreDto(val name: String)

@JsonClass(generateAdapter = true)
data class RatingDto(
    val value: Double? = null,
    @param:Json(name = "votes-count") val votesCount: Int = 0
)
