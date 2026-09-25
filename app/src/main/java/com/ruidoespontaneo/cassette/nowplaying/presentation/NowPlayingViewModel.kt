package com.ruidoespontaneo.cassette.nowplaying.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayback
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayer
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewQueue
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics
import com.ruidoespontaneo.cassette.lyrics.domain.usecase.GetTrackLyricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * What's playing app-wide, for the floating toolbar's now-playing button and its dialog: the
 * [PreviewQueue]'s current track plus the [PreviewPlayer]'s state, and stop / play-again controls.
 * The track's lyrics follow it: each new track starts a fresh lookup, and one still running for the
 * previous track is dropped.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val previewQueue: PreviewQueue,
    previewPlayer: PreviewPlayer,
    private val getTrackLyrics: GetTrackLyricsUseCase
) : MviViewModel<NowPlayingUiState, NowPlayingIntent, NowPlayingEffect>(NowPlayingUiState()) {

    init {
        viewModelScope.launch {
            combine(previewQueue.nowPlaying, previewPlayer.playback, previewQueue.isAdvancing) { nowPlaying, playback, isAdvancing ->
                val status = when {
                    playback?.status == PreviewPlayback.Status.Playing -> NowPlayingStatus.Playing
                    playback != null || isAdvancing -> NowPlayingStatus.Loading
                    else -> NowPlayingStatus.Stopped
                }
                nowPlaying to status
            }.collect { (nowPlaying, status) -> setState { copy(nowPlaying = nowPlaying, status = status) } }
        }
        viewModelScope.launch {
            previewQueue.nowPlaying
                .filterNotNull()
                .map { it.album to it.position }
                .distinctUntilChanged()
                .collectLatest { (album, position) ->
                    setState { copy(lyrics = LyricsUiState.Loading) }
                    val lyrics = getTrackLyrics(album, position).fold(
                        onSuccess = { it.toUiState() },
                        onFailure = { LyricsUiState.Failed }
                    )
                    setState { copy(lyrics = lyrics) }
                }
        }
    }

    override fun onIntent(intent: NowPlayingIntent) {
        when (intent) {
            NowPlayingIntent.Stop -> previewQueue.stop()
            NowPlayingIntent.Replay -> previewQueue.replay()
            NowPlayingIntent.Next -> previewQueue.skipToNext()
            NowPlayingIntent.Previous -> previewQueue.skipToPrevious()
        }
    }
}

private fun Lyrics?.toUiState(): LyricsUiState = when (this) {
    is Lyrics.Plain -> LyricsUiState.Found(text)
    Lyrics.Instrumental -> LyricsUiState.Instrumental
    null -> LyricsUiState.NotFound
}
