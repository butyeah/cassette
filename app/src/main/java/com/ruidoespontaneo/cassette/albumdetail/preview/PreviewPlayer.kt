package com.ruidoespontaneo.cassette.albumdetail.preview

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays one iTunes preview clip at a time, app-wide. AlbumPagerScreen hosts many albums'
 * AlbumDetailViewModels at once, so a single shared player is what guarantees that starting one
 * album's preview stops another's — see [Media3PreviewPlayer].
 *
 * Call from the main thread.
 */
interface PreviewPlayer {

    /** What's playing right now, or `null` when idle. */
    val playback: StateFlow<PreviewPlayback?>

    /**
     * The URL of each clip that played through to its end — never one that was [stop]ped,
     * interrupted or failed. Emitted after [playback] has gone back to `null`, so a collector can
     * [play] the next clip straight away.
     */
    val completions: SharedFlow<String>

    /**
     * Starts [url], stopping whatever was playing before it. [metadata] describes it to the system
     * (the media notification, lock screen, Bluetooth displays).
     */
    fun play(url: String, metadata: PreviewMetadata? = null)

    /** Stops playback, if any. */
    fun stop()
}

/** What a clip is, for the system's media surfaces — the player itself doesn't need it. */
data class PreviewMetadata(
    val trackTitle: String?,
    val artistName: String,
    val albumTitle: String,
    val artworkUrl: String
)

/**
 * The clip at [url] and how far along it is. Keyed by URL rather than by album and track, since a
 * clip's URL already identifies it uniquely and the player knows nothing about albums.
 * [remainingMs] is `null` until the clip's duration is known.
 */
data class PreviewPlayback(val url: String, val status: Status, val remainingMs: Long? = null) {
    enum class Status {
        /** Buffering, not audible yet. */
        Loading,
        Playing
    }
}
