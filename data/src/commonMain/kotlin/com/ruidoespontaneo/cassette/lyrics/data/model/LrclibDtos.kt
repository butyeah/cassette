package com.ruidoespontaneo.cassette.lyrics.data.model

import kotlinx.serialization.Serializable

/**
 * One track from LRCLIB's `GET /get` (a single object) or `GET /search` (a list of them). Only the
 * fields the app uses; `syncedLyrics` is skipped, since timestamps can't line up with iTunes'
 * mid-song previews.
 */
@Serializable
data class LrclibTrack(
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    /** Seconds. */
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null
)
