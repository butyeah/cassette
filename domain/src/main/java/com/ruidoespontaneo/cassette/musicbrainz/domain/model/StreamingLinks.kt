package com.ruidoespontaneo.cassette.musicbrainz.domain.model

/**
 * Where to listen to an [AlbumDetail] on a streaming service, when MusicBrainz has that
 * relationship recorded for it. Sourced entirely from the offline Firestore cache (see
 * [com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository]) — there's no live
 * MusicBrainz fallback for this, so a `null` field just means "no link on file", not "not
 * loaded yet".
 */
data class StreamingLinks(
    val spotify: String? = null,
    val appleMusic: String? = null,
    val youtubeMusic: String? = null
)
