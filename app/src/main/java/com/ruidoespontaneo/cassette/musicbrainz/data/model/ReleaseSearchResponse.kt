package com.ruidoespontaneo.cassette.musicbrainz.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response body of `GET /ws/2/release?query=...`. */
@JsonClass(generateAdapter = true)
data class ReleaseSearchResponse(
    val count: Int,
    val offset: Int,
    val releases: List<ReleaseDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReleaseDto(
    val id: String,
    val title: String,
    // ISO 8601, but MusicBrainz allows partial dates ("2024" or "2024-01")
    // when a release's day or month isn't known.
    val date: String? = null,
    val country: String? = null,
    @param:Json(name = "artist-credit") val artistCredit: List<ArtistCreditDto> = emptyList(),
    @param:Json(name = "release-group") val releaseGroup: ReleaseGroupDto? = null
)

@JsonClass(generateAdapter = true)
data class ArtistCreditDto(
    val name: String,
    // Text joining this credit to the next one, e.g. " feat. " — needed to
    // render multi-artist credits the way MusicBrainz does.
    @param:Json(name = "joinphrase") val joinPhrase: String? = null,
    val artist: ArtistCreditArtistDto? = null
)

@JsonClass(generateAdapter = true)
data class ArtistCreditArtistDto(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class ReleaseGroupDto(
    val id: String,
    @param:Json(name = "primary-type") val primaryType: String? = null
)
