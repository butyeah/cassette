package com.ruidoespontaneo.cassette.musicbrainz.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response body of `GET /ws/2/release-group/{id}?inc=artist-credits+genres+ratings`. */
@Serializable
data class ReleaseGroupDetailDto(
    val id: String,
    val title: String,
    @SerialName("primary-type") val primaryType: String? = null,
    // ISO 8601, but MusicBrainz allows partial dates ("2024" or "2024-01")
    // when a release group's first release's day or month isn't known.
    @SerialName("first-release-date") val firstReleaseDate: String? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCreditDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val rating: RatingDto? = null
)

@Serializable
data class GenreDto(val name: String)

@Serializable
data class RatingDto(
    val value: Double? = null,
    @SerialName("votes-count") val votesCount: Int = 0
)
