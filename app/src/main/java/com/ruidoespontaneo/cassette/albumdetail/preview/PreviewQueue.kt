package com.ruidoespontaneo.cassette.albumdetail.preview

import com.ruidoespontaneo.cassette.core.di.ApplicationScope
import com.ruidoespontaneo.cassette.itunes.domain.usecase.GetTrackPreviewsUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The track [PreviewQueue] last started, with what it needs to carry on from there: the album's
 * [previews] (by track position) and the [dayAlbumIds] autoplay continues through.
 */
data class NowPlaying(
    val album: AlbumDetail,
    val previews: Map<Int, String>,
    val position: Int,
    val dayAlbumIds: List<String>
) {
    val trackTitle: String? get() = album.tracks.firstOrNull { it.position == position }?.title

    private val albumIndex: Int get() = dayAlbumIds.indexOf(album.id)

    /** Whether ⏭ can go anywhere: a later preview here, or a later album of the day. */
    val hasNext: Boolean
        get() = previews.keys.any { it > position } || (albumIndex >= 0 && albumIndex < dayAlbumIds.lastIndex)

    /** Whether ⏮ can go anywhere: an earlier preview here, or an earlier album of the day. */
    val hasPrevious: Boolean
        get() = previews.keys.any { it < position } || albumIndex > 0
}

/**
 * Autoplay, app-wide. Screens start a track through [play]; when a clip plays to its end, the queue
 * starts the album's next track that has a preview, then the first preview of each following album
 * of the day — loading those albums itself, so playback carries on after the album pager is gone.
 * It stops after the day's last album.
 *
 * [nowPlaying] survives [stop] so the track can be [replay]ed; it only changes when another track
 * starts. Call from the main thread, like [PreviewPlayer].
 */
@Singleton
class PreviewQueue @Inject constructor(
    private val player: PreviewPlayer,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val getTrackPreviewsUseCase: GetTrackPreviewsUseCase,
    @param:ApplicationScope private val scope: CoroutineScope
) {

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _isAdvancing = MutableStateFlow(false)

    /** Whether the queue is between albums, looking up the next (or previous) one to play. */
    val isAdvancing: StateFlow<Boolean> = _isAdvancing.asStateFlow()

    private var advanceJob: Job? = null

    // Set by stop(): a completion already on its way (the clip ended just as it was stopped) must
    // not carry autoplay on. Cleared whenever a track starts.
    private var isStopped = true

    init {
        scope.launch { player.completions.collect(::onCompleted) }
    }

    /** Plays [album]'s preview at [position], if it has one, and continues through [dayAlbumIds]. */
    fun play(album: AlbumDetail, previews: Map<Int, String>, position: Int, dayAlbumIds: List<String>) {
        cancelAdvance()
        start(NowPlaying(album, previews, position, dayAlbumIds))
    }

    /** Stops playback and any lookup of the next album; [nowPlaying] stays for [replay]. */
    fun stop() {
        isStopped = true
        cancelAdvance()
        player.stop()
    }

    /** Plays [nowPlaying]'s track again from the start, autoplay included. */
    fun replay() {
        val current = _nowPlaying.value ?: return
        cancelAdvance()
        start(current)
    }

    /**
     * Plays the next previewable track: the rest of this album, then the first preview of the
     * day's next album that has any. With nothing left, playback stops.
     */
    fun skipToNext() {
        val current = _nowPlaying.value ?: return
        cancelAdvance()
        val nextPosition = current.previews.keys.filter { it > current.position }.minOrNull()
        if (nextPosition != null) {
            start(current.copy(position = nextPosition))
        } else {
            advanceJob = scope.launch { playAdjacentAlbum(current, forward = true) }
        }
    }

    /**
     * Plays the previous previewable track: earlier in this album, then the **last** preview of the
     * day's previous album that has any. With nothing earlier, the current track carries on.
     */
    fun skipToPrevious() {
        val current = _nowPlaying.value ?: return
        cancelAdvance()
        val previousPosition = current.previews.keys.filter { it < current.position }.maxOrNull()
        if (previousPosition != null) {
            start(current.copy(position = previousPosition))
        } else {
            advanceJob = scope.launch { playAdjacentAlbum(current, forward = false) }
        }
    }

    private fun start(next: NowPlaying) {
        val url = next.previews[next.position] ?: return
        isStopped = false
        _nowPlaying.value = next
        player.play(url)
    }

    private fun cancelAdvance() {
        advanceJob?.cancel()
        advanceJob = null
        _isAdvancing.value = false
    }

    private fun onCompleted(url: String) {
        val current = _nowPlaying.value ?: return
        if (isStopped || current.previews[current.position] != url) return
        skipToNext()
    }

    /**
     * Plays the nearest album after (or, if not [forward], before) [current]'s that loads and has
     * previews — from its first preview going forward, its last going back. Albums that fail to
     * load or have nothing to preview are skipped. The current clip keeps playing meanwhile.
     */
    private suspend fun playAdjacentAlbum(current: NowPlaying, forward: Boolean) {
        val index = current.dayAlbumIds.indexOf(current.album.id)
        if (index < 0) return
        val candidates = if (forward) {
            current.dayAlbumIds.drop(index + 1)
        } else {
            current.dayAlbumIds.take(index).asReversed()
        }
        _isAdvancing.value = true
        try {
            for (albumId in candidates) {
                val album = getAlbumDetailUseCase(albumId).getOrNull() ?: continue
                val previews = getTrackPreviewsUseCase(album).getOrNull().orEmpty()
                val position = (if (forward) previews.keys.minOrNull() else previews.keys.maxOrNull()) ?: continue
                start(NowPlaying(album, previews, position, current.dayAlbumIds))
                return
            }
            // Past the day's last album there's nothing more to play; before its first, the
            // current track just carries on.
            if (forward) {
                isStopped = true
                player.stop()
            }
        } finally {
            _isAdvancing.value = false
        }
    }
}
