package com.ruidoespontaneo.cassette.albumdetail.preview

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ruidoespontaneo.cassette.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * Keeps previews playing — and autoplay moving on — while the app is in the background. Media3
 * promotes it to a foreground service with a media notification (cover, track, album) while a clip
 * plays; the session wraps [Media3PreviewPlayer]'s one ExoPlayer, which outlives this service.
 *
 * Started by [Media3PreviewPlayer.play] via [startIfNeeded]; stops itself when the app's task is
 * removed with nothing playing.
 */
@AndroidEntryPoint
class PreviewPlaybackService : MediaSessionService() {

    @Inject
    lateinit var previewPlayer: Media3PreviewPlayer

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, previewPlayer.exoPlayer)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        val isPlaying = player != null && player.playWhenReady && player.mediaItemCount > 0 &&
            player.playbackState != Player.STATE_ENDED
        if (!isPlaying) stopSelf()
    }

    override fun onDestroy() {
        // The player belongs to Media3PreviewPlayer and outlives this service — only the session goes.
        session?.release()
        session = null
        isRunning = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var isRunning = false

        /**
         * Starts the service unless it's already up. Only the first clip starts it, and that one
         * is always a tap in the foreground — later clips (autoplay, possibly in the background)
         * find it running.
         */
        fun startIfNeeded(context: Context) {
            if (isRunning) return
            try {
                context.startService(Intent(context, PreviewPlaybackService::class.java))
            } catch (e: IllegalStateException) {
                // Background start restrictions: playback goes on, just without the notification.
                Timber.w(e, "Couldn't start the preview playback service")
            }
        }
    }
}
