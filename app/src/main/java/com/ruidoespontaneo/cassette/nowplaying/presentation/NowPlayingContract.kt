package com.ruidoespontaneo.cassette.nowplaying.presentation

import com.ruidoespontaneo.cassette.albumdetail.preview.NowPlaying
import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState

data class NowPlayingUiState(
    /** The track the queue last started — kept after it stops, so it can be played again. */
    val nowPlaying: NowPlaying? = null,
    val status: NowPlayingStatus = NowPlayingStatus.Stopped
) : UiState {
    /** Whether something is loaded: buffering, playing, or autoplay looking up the next album. */
    val isActive: Boolean get() = status != NowPlayingStatus.Stopped
}

enum class NowPlayingStatus {
    Stopped,

    /** Buffering the clip, or autoplay looking up the next album. */
    Loading,
    Playing
}

sealed interface NowPlayingIntent : UiIntent {
    data object Stop : NowPlayingIntent

    /** Plays the current track again from the start, autoplay included. */
    data object Replay : NowPlayingIntent

    /** The next previewable track, crossing into the day's next album. */
    data object Next : NowPlayingIntent

    /** The previous previewable track, crossing back to the previous album's last one. */
    data object Previous : NowPlayingIntent
}

// No one-off events — this is here so NowPlayingViewModel has a concrete UiEffect to declare.
sealed interface NowPlayingEffect : UiEffect
