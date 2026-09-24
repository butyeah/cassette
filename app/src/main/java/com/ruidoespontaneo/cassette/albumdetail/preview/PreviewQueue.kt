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

    /** Whether the queue is between albums, looking up the next one to play. */
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
        val nextPosition = current.previews.keys.filter { it > current.position }.minOrNull()
        if (nextPosition != null) {
            start(current.copy(position = nextPosition))
        } else {
            advanceJob = scope.launch { playNextAlbumAfter(current) }
        }
    }

    private suspend fun playNextAlbumAfter(current: NowPlaying) {
        val index = current.dayAlbumIds.indexOf(current.album.id)
        if (index < 0) return
        _isAdvancing.value = true
        try {
            for (albumId in current.dayAlbumIds.drop(index + 1)) {
                // Albums that fail to load, or have nothing to preview, are skipped.
                val album = getAlbumDetailUseCase(albumId).getOrNull() ?: continue
                val previews = getTrackPreviewsUseCase(album).getOrNull().orEmpty()
                val first = previews.keys.minOrNull() ?: continue
                start(NowPlaying(album, previews, first, current.dayAlbumIds))
                return
            }
        } finally {
            _isAdvancing.value = false
        }
    }
}
