package com.ruidoespontaneo.cassette.musicbrainz.domain.model

/**
 * One track on an [AlbumDetail]'s tracklist.
 *
 * Tracks belong to a specific MusicBrainz release, not to the release group
 * `AlbumDetail` is looked up by — see
 * [com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository.getAlbumDetail]
 * for how one release is chosen to represent the group's tracklist.
 */
data class Track(
    val position: Int,
    val title: String,
    /** `null` when MusicBrainz has no duration recorded for this track. */
    val lengthMs: Int?
)
