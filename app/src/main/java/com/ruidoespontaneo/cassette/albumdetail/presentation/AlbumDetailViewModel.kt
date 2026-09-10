package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Nav-graph argument name for the album's MBID. The nav graph itself is wired up in a follow-up
 * PR; this constant is defined here so the route destination can reuse it verbatim.
 */
const val ALBUM_DETAIL_ARG_ALBUM_ID = "albumId"

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase
) : MviViewModel<AlbumDetailUiState, AlbumDetailIntent, AlbumDetailEffect>(
    AlbumDetailUiState()
) {

    private val albumId: String = checkNotNull(savedStateHandle[ALBUM_DETAIL_ARG_ALBUM_ID]) {
        "AlbumDetailViewModel requires a non-null $ALBUM_DETAIL_ARG_ALBUM_ID nav argument"
    }

    init {
        loadAlbum()
    }

    override fun onIntent(intent: AlbumDetailIntent) {
        when (intent) {
            AlbumDetailIntent.Retry -> loadAlbum()
        }
    }

    private fun loadAlbum() {
        setState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAlbumDetailUseCase(albumId)
                .onSuccess { album -> setState { copy(isLoading = false, album = album) } }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't load album")
                    }
                }
        }
    }
}
