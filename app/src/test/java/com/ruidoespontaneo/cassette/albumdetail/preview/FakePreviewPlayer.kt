package com.ruidoespontaneo.cassette.albumdetail.preview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records what a ViewModel asks of the player and lets a test drive the playback state directly. */
class FakePreviewPlayer : PreviewPlayer {

    private val _playback = MutableStateFlow<PreviewPlayback?>(null)
    override val playback: StateFlow<PreviewPlayback?> = _playback.asStateFlow()

    val played = mutableListOf<String>()
    var stopCount = 0
        private set

    override fun play(url: String) {
        played += url
        _playback.value = PreviewPlayback(url, PreviewPlayback.Status.Loading)
    }

    override fun stop() {
        stopCount++
        _playback.value = null
    }

    fun setStatus(status: PreviewPlayback.Status) {
        _playback.value = _playback.value?.copy(status = status)
    }

    fun setRemaining(ms: Long) {
        _playback.value = _playback.value?.copy(remainingMs = ms)
    }
}
