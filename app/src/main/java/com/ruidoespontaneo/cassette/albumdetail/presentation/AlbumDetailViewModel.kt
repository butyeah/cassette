package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/**
 * One instance per album, constructed with an explicit [albumId] rather than pulling it from a
 * [androidx.lifecycle.SavedStateHandle] nav argument — AlbumPagerScreen hosts many albums' worth
 * of this ViewModel inside a single nav destination (one per HorizontalPager page), so there's no
 * one-to-one nav-arg-to-ViewModel relationship to rely on. Built via [Factory] and
 * `androidx.hilt.navigation.compose.hiltViewModel`'s assisted-injection overload, keyed by
 * [albumId] so each page keeps its own instance (and its already-loaded state) for as long as the
 * pager's nav destination is on the back stack.
 */
@HiltViewModel(assistedFactory = AlbumDetailViewModel.Factory::class)
class AlbumDetailViewModel @AssistedInject constructor(
    @Assisted private val albumId: String,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase
) : MviViewModel<AlbumDetailUiState, AlbumDetailIntent, AlbumDetailEffect>(
    AlbumDetailUiState()
) {

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumDetailViewModel
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
