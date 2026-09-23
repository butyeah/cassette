package com.ruidoespontaneo.cassette.albumdetail.preview

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records what a ViewModel asks of the player and lets a test drive the playback state directly. */
class FakePreviewPlayer : PreviewPlayer {

    private val _playback = MutableStateFlow<PreviewPlayback?>(null)
    override val playback: StateFlow<PreviewPlayback?> = _playback.asStateFlow()

    private val _completions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val completions: SharedFlow<String> = _completions.asSharedFlow()

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

    /** Plays the current clip through to its end, the way [Media3PreviewPlayer] reports it. */
    fun complete() {
        val url = checkNotNull(_playback.value) { "nothing is playing" }.url
        _playback.value = null
        _completions.tryEmit(url)
    }

    fun setRemaining(ms: Long) {
        _playback.value = _playback.value?.copy(remainingMs = ms)
    }
}
