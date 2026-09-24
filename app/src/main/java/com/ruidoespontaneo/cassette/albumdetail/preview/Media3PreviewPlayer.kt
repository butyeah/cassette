package com.ruidoespontaneo.cassette.albumdetail.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
 * [PreviewPlayer] backed by one Media3 [ExoPlayer], built on the first [play] and kept for the
 * app's lifetime: [PreviewPlaybackService]'s media session wraps it, and a session needs a single
 * player to follow from clip to clip. Between clips it holds no decoder — [stop] returns it to
 * idle, which releases its media resources and audio focus.
 *
 * Each [play] also makes sure [PreviewPlaybackService] is running, so playback (and autoplay) carry
 * on in the background with a media notification.
 *
 * Anything that would leave the clip paused — losing audio focus to a call, headphones
 * unplugging, the notification's pause button — ends playback instead of pausing it; resuming a
 * 30-second preview isn't worth the extra state.
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

    /** The one ExoPlayer, built on first use — for [PreviewPlaybackService]'s media session. */
    val exoPlayer: ExoPlayer
        get() = player ?: buildPlayer().also { player = it }

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

    override fun play(url: String, metadata: PreviewMetadata?) {
        progressHandler.removeCallbacks(progressTick)
        // Set before touching the player, so the listener treats events from here on as this clip's.
        _playback.value = PreviewPlayback(url, PreviewPlayback.Status.Loading)
        exoPlayer.apply {
            setMediaItem(mediaItem(url, metadata))
            playWhenReady = true
            prepare()
        }
        PreviewPlaybackService.startIfNeeded(context)
    }

    override fun stop() {
        progressHandler.removeCallbacks(progressTick)
        // Cleared first: the listener ignores the events stopping the player raises.
        _playback.value = null
        player?.apply {
            stop()
            clearMediaItems()
        }
    }

    private fun buildPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also { it.addListener(listener) }

    private fun mediaItem(url: String, metadata: PreviewMetadata?): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (metadata != null) {
            builder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(metadata.trackTitle)
                    .setArtist(metadata.artistName)
                    .setAlbumTitle(metadata.albumTitle)
                    .setArtworkUri(metadata.artworkUrl.toUri())
                    .build()
            )
        }
        return builder.build()
    }

    /** Only acts while a clip is loaded — [stop] clears [playback] before the player goes idle. */
    private val listener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (_playback.value == null) return
            val exoPlayer = player ?: return
            when (playbackState) {
                Player.STATE_READY -> if (exoPlayer.playWhenReady) setStatus(PreviewPlayback.Status.Playing)
                Player.STATE_ENDED -> complete()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (_playback.value == null) return
            val exoPlayer = player ?: return
            if (playWhenReady) {
                if (exoPlayer.playbackState == Player.STATE_READY) setStatus(PreviewPlayback.Status.Playing)
            } else {
                stop()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (_playback.value == null) return
            Timber.e(error, "Preview playback failed")
            stop()
        }
    }

    // Announce after clearing playback, so a collector that plays the next clip straight away isn't
    // undone. The player itself is left in its ended state rather than stopped: it keeps wanting to
    // play, which keeps the playback service in the foreground for the next clip autoplay starts.
    private fun complete() {
        val url = _playback.value?.url ?: return
        progressHandler.removeCallbacks(progressTick)
        _playback.value = null
        _completions.tryEmit(url)
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
