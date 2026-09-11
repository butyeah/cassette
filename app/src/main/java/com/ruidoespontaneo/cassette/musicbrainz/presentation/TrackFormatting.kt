package com.ruidoespontaneo.cassette.musicbrainz.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track

/**
 * `m:ss` duration for [this] track, as shown next to its title on AlbumDetailScreen.
 * `null` when MusicBrainz has no duration recorded for it, so the row can omit it.
 */
fun Track.durationText(): String? {
    val lengthMs = lengthMs ?: return null
    val totalSeconds = lengthMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
