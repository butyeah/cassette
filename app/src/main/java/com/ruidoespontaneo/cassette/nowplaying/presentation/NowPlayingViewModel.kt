package com.ruidoespontaneo.cassette.nowplaying.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayback
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayer
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewQueue
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * What's playing app-wide, for the floating toolbar's now-playing button and its dialog: the
 * [PreviewQueue]'s current track plus the [PreviewPlayer]'s state, and stop / play-again controls.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val previewQueue: PreviewQueue,
    previewPlayer: PreviewPlayer
) : MviViewModel<NowPlayingUiState, NowPlayingIntent, NowPlayingEffect>(NowPlayingUiState()) {

    init {
        viewModelScope.launch {
            combine(previewQueue.nowPlaying, previewPlayer.playback, previewQueue.isAdvancing) { nowPlaying, playback, isAdvancing ->
                val status = when {
                    playback?.status == PreviewPlayback.Status.Playing -> NowPlayingStatus.Playing
                    playback != null || isAdvancing -> NowPlayingStatus.Loading
                    else -> NowPlayingStatus.Stopped
                }
                NowPlayingUiState(nowPlaying, status)
            }.collect { state -> setState { state } }
        }
    }

    override fun onIntent(intent: NowPlayingIntent) {
        when (intent) {
            NowPlayingIntent.Stop -> previewQueue.stop()
            NowPlayingIntent.Replay -> previewQueue.replay()
        }
    }
}
