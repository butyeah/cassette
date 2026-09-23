package com.ruidoespontaneo.cassette.albumdetail.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * [PreviewPlayer] backed by a Media3 [ExoPlayer]. An ExoPlayer exists only while a clip is in
 * flight: one is built per [play] and released as soon as the clip ends, errors, or is stopped.
 * That keeps a decoder and audio focus from lingering for the life of the app, at the price of
 * setting a player up per 30-second clip — cheap next to the network fetch.
 *
 * Anything that would leave the clip paused — losing audio focus to a call, headphones
 * unplugging — ends playback instead of pausing it; resuming a 30-second preview isn't worth the
 * extra state.
 */
@Singleton
class Media3PreviewPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PreviewPlayer {

    private val _playback = MutableStateFlow<PreviewPlayback?>(null)
    override val playback: StateFlow<PreviewPlayback?> = _playback.asStateFlow()

    private val _completions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val completions: SharedFlow<String> = _completions.asSharedFlow()

    private var player: ExoPlayer? = null

    // ExoPlayer has no position callback, so remaining time is polled while a clip is audible.
    // Everything here already runs on the main thread, which is where ExoPlayer must be read from.
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressTick = object : Runnable {
        override fun run() {
            val exoPlayer = player ?: return
            if (exoPlayer.duration != C.TIME_UNSET) {
                val remaining = (exoPlayer.duration - exoPlayer.currentPosition).coerceAtLeast(0)
                _playback.value = _playback.value?.copy(remainingMs = remaining)
            }
            progressHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    override fun play(url: String) {
        stop()
        val exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addListener(listenerFor(exoPlayer))
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
        player = exoPlayer
        _playback.value = PreviewPlayback(url, PreviewPlayback.Status.Loading)
    }

    override fun stop() {
        progressHandler.removeCallbacks(progressTick)
        player?.release()
        player = null
        _playback.value = null
    }

    // Stop first, then announce: a collector that plays the next clip in response must not have
    // it torn down by this clip's own stop().
    private fun complete() {
        val url = _playback.value?.url
        stop()
        if (url != null) _completions.tryEmit(url)
    }

    /** Ignores events from any player other than the current one — see [stop]. */
    private fun listenerFor(exoPlayer: ExoPlayer) = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (player !== exoPlayer) return
            when (playbackState) {
                Player.STATE_READY -> if (exoPlayer.playWhenReady) setStatus(PreviewPlayback.Status.Playing)
                Player.STATE_ENDED -> complete()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (player !== exoPlayer) return
            if (playWhenReady) {
                if (exoPlayer.playbackState == Player.STATE_READY) setStatus(PreviewPlayback.Status.Playing)
            } else {
                stop()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (player !== exoPlayer) return
            Timber.e(error, "Preview playback failed")
            stop()
        }
    }

    private fun setStatus(status: PreviewPlayback.Status) {
        _playback.value = _playback.value?.copy(status = status)
        if (status == PreviewPlayback.Status.Playing) {
            progressHandler.removeCallbacks(progressTick)
            progressHandler.post(progressTick)
        }
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 250L
    }
}
