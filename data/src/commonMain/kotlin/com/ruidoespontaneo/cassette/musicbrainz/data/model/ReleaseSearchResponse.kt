package com.ruidoespontaneo.cassette.musicbrainz.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response body of `GET /ws/2/release?query=...`. */
@Serializable
data class ReleaseSearchResponse(
    val count: Int,
    val offset: Int,
    val releases: List<ReleaseDto> = emptyList()
)

@Serializable
data class ReleaseDto(
    val id: String,
    val title: String,
    // ISO 8601, but MusicBrainz allows partial dates ("2024" or "2024-01")
    // when a release's day or month isn't known.
    val date: String? = null,
    val country: String? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCreditDto> = emptyList(),
    @SerialName("release-group") val releaseGroup: ReleaseGroupDto? = null
)

@Serializable
data class ArtistCreditDto(
    val name: String,
    // Text joining this credit to the next one, e.g. " feat. " — needed to
    // render multi-artist credits the way MusicBrainz does.
    @SerialName("joinphrase") val joinPhrase: String? = null,
    val artist: ArtistCreditArtistDto? = null
)

@Serializable
data class ArtistCreditArtistDto(
    val id: String,
    val name: String
)

@Serializable
data class ReleaseGroupDto(
    val id: String,
    @SerialName("primary-type") val primaryType: String? = null
)
