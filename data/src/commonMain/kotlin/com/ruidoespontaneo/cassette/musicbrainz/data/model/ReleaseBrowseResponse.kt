package com.ruidoespontaneo.cassette.musicbrainz.data.model

import kotlinx.serialization.Serializable

/** Response body of `GET /ws/2/release?release-group=...&inc=recordings`. */
@Serializable
data class ReleaseBrowseResponse(
    val releases: List<ReleaseWithMediaDto> = emptyList()
)

@Serializable
data class ReleaseWithMediaDto(
    val id: String,
    val media: List<MediaDto> = emptyList()
)

@Serializable
data class MediaDto(
    val tracks: List<TrackDto> = emptyList()
)

@Serializable
data class TrackDto(
    val position: Int,
    val title: String,
    // Milliseconds; null when MusicBrainz has no duration recorded for this track.
    val length: Int? = null
)
