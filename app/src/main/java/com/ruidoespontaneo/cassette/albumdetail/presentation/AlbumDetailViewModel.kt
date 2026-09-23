package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayback
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayer
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.itunes.domain.usecase.GetTrackPreviewsUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/**
 * One instance per album, constructed with an explicit [albumId] rather than pulling it from a
 * [androidx.lifecycle.SavedStateHandle] nav argument — AlbumPagerScreen hosts many albums' worth
 * of this ViewModel inside a single nav destination (one per VerticalPager page), so there's no
 * one-to-one nav-arg-to-ViewModel relationship to rely on. Built via [Factory] and
 * `androidx.hilt.navigation.compose.hiltViewModel`'s assisted-injection overload, keyed by
 * [albumId] so each page keeps its own instance (and its already-loaded state) for as long as the
 * pager's nav destination is on the back stack.
 *
 * Playback itself belongs to the app-wide [PreviewPlayer] (one clip at a time across every page);
 * this ViewModel only starts and stops clips for its own album and mirrors the player's state back
 * into [AlbumDetailUiState.previewPlayback] when the playing clip is one of its own. Stopping when
 * the pager is left or paged away from is AlbumPagerViewModel's job.
 *
 * Auto-play: when one of its clips plays to the end, it starts the next track that has a preview;
 * after the last one it sends [AlbumDetailEffect.TracklistFinished] so the pager can move on to the
 * next album, which it starts with [AlbumDetailIntent.AutoPlay].
 */
@HiltViewModel(assistedFactory = AlbumDetailViewModel.Factory::class)
class AlbumDetailViewModel @AssistedInject constructor(
    @Assisted private val albumId: String,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val getTrackPreviewsUseCase: GetTrackPreviewsUseCase,
    private val previewPlayer: PreviewPlayer
) : MviViewModel<AlbumDetailUiState, AlbumDetailIntent, AlbumDetailEffect>(
    AlbumDetailUiState()
) {

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumDetailViewModel
    }

    /** Set by [AlbumDetailIntent.AutoPlay] while previews are still loading. */
    private var autoPlayPending = false

    /** Whether the preview lookup has finished, successfully or not. */
    private var previewsKnown = false

    init {
        loadAlbum()
        observePlayback()
        observeCompletions()
    }

    override fun onIntent(intent: AlbumDetailIntent) {
        when (intent) {
            AlbumDetailIntent.Retry -> loadAlbum()
            is AlbumDetailIntent.TogglePreview -> togglePreview(intent.trackPosition)
            AlbumDetailIntent.AutoPlay -> autoPlay()
        }
    }

    private fun loadAlbum() {
        previewsKnown = false
        setState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAlbumDetailUseCase(albumId)
                .onSuccess { album ->
                    setState { copy(isLoading = false, album = album) }
                    loadPreviews(album)
                }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't load album")
                    }
                    // Nothing to play here, so auto-play shouldn't stall on the error screen.
                    onPreviewsKnown()
                }
        }
    }

    // A second, independent step: the album is already on screen, so a slow or failed iTunes lookup
    // only ever means "no play buttons" — never the error screen. (The repository already logs a
    // failure.)
    private fun loadPreviews(album: AlbumDetail) {
        viewModelScope.launch {
            getTrackPreviewsUseCase(album).onSuccess { previews -> setState { copy(previews = previews) } }
            onPreviewsKnown()
        }
    }

    private fun onPreviewsKnown() {
        previewsKnown = true
        if (autoPlayPending) {
            autoPlayPending = false
            autoPlay()
        }
    }

    private fun autoPlay() {
        if (!previewsKnown) {
            autoPlayPending = true
            return
        }
        val first = currentState.previews.minByOrNull { it.key }
        if (first != null) {
            previewPlayer.play(first.value)
        } else {
            sendEffect { AlbumDetailEffect.TracklistFinished }
        }
    }

    private fun observeCompletions() {
        viewModelScope.launch {
            previewPlayer.completions.collect { url -> playNextAfter(url) }
        }
    }

    /** Only reacts to [url] if it's one of this album's clips — the player is shared. */
    private fun playNextAfter(url: String) {
        val previews = currentState.previews
        val position = previews.entries.firstOrNull { it.value == url }?.key ?: return
        val next = previews.filterKeys { it > position }.minByOrNull { it.key }
        if (next != null) {
            previewPlayer.play(next.value)
        } else {
            sendEffect { AlbumDetailEffect.TracklistFinished }
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            previewPlayer.playback.collect { playback ->
                setState { copy(previewPlayback = trackPlaybackFor(playback)) }
            }
        }
    }

    private fun togglePreview(trackPosition: Int) {
        val url = currentState.previews[trackPosition] ?: return
        if (currentState.previewPlayback?.position == trackPosition) {
            previewPlayer.stop()
        } else {
            previewPlayer.play(url)
        }
    }

    /** `null` unless [playback] is a clip of this album — the player is shared with every other page. */
    private fun AlbumDetailUiState.trackPlaybackFor(playback: PreviewPlayback?): TrackPlayback? {
        if (playback == null) return null
        val position = previews.entries.firstOrNull { it.value == playback.url }?.key ?: return null
        return TrackPlayback(
            position,
            isLoading = playback.status == PreviewPlayback.Status.Loading,
            remainingMs = playback.remainingMs
        )
    }
}
