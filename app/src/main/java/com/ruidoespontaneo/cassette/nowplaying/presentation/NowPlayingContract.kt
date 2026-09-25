package com.ruidoespontaneo.cassette.nowplaying.presentation

import com.ruidoespontaneo.cassette.albumdetail.preview.NowPlaying
import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState

data class NowPlayingUiState(
    /** The track the queue last started — kept after it stops, so it can be played again. */
    val nowPlaying: NowPlaying? = null,
    val status: NowPlayingStatus = NowPlayingStatus.Stopped,
    /** [nowPlaying]'s lyrics, looked up again whenever the track changes. */
    val lyrics: LyricsUiState = LyricsUiState.Loading
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

sealed interface LyricsUiState {
    data object Loading : LyricsUiState

    data class Found(val text: String) : LyricsUiState

    data object Instrumental : LyricsUiState

    /** LRCLIB has nothing for this track. */
    data object NotFound : LyricsUiState

    /** The lookup itself failed (offline, say). */
    data object Failed : LyricsUiState
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
