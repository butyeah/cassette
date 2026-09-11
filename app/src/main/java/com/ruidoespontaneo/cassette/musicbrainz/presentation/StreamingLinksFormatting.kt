package com.ruidoespontaneo.cassette.musicbrainz.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks

/** True when at least one streaming-service link is cached for this album — AlbumDetailScreen
 *  only renders its streaming-links row when this is true. */
fun StreamingLinks.hasAny(): Boolean = spotify != null || appleMusic != null || youtubeMusic != null

/**
 * (display label, url) for each cached link, in a fixed display order — what
 * AlbumDetailScreen's streaming-links row renders one chip per.
 */
fun StreamingLinks.asDisplayList(): List<Pair<String, String>> = buildList {
    spotify?.let { add("Spotify" to it) }
    appleMusic?.let { add("Apple Music" to it) }
    youtubeMusic?.let { add("YouTube Music" to it) }
}
