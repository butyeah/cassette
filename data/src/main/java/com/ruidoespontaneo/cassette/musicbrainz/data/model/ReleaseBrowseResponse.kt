package com.ruidoespontaneo.cassette.musicbrainz.data.model

import com.squareup.moshi.JsonClass

/** Response body of `GET /ws/2/release?release-group=...&inc=recordings`. */
@JsonClass(generateAdapter = true)
data class ReleaseBrowseResponse(
    val releases: List<ReleaseWithMediaDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReleaseWithMediaDto(
    val id: String,
    val media: List<MediaDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MediaDto(
    val tracks: List<TrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TrackDto(
    val position: Int,
    val title: String,
    // Milliseconds; null when MusicBrainz has no duration recorded for this track.
    val length: Int? = null
)
