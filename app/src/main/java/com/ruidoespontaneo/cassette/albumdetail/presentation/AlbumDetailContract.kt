package com.ruidoespontaneo.cassette.albumdetail.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val album: AlbumDetail? = null,
    val errorMessage: String? = null
) : UiState

sealed interface AlbumDetailIntent : UiIntent {
    data object Retry : AlbumDetailIntent
}

// No one-off events yet (nothing to navigate to or pop a snackbar for) —
// this is here so AlbumDetailViewModel has a concrete UiEffect to declare.
sealed interface AlbumDetailEffect : UiEffect
