package com.ruidoespontaneo.cassette.albumpager.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState

data class AlbumPagerUiState(
    val isLoading: Boolean = true,
    /** Every album released on the day, in the same order as the Daily screen's list — see
     *  [AlbumPagerViewModel]. One [com.ruidoespontaneo.cassette.albumdetail.presentation.AlbumDetailScreen]
     *  page per id. */
    val albumIds: List<String> = emptyList(),
    /** Index into [albumIds] the pager should open on — the album that was actually tapped. */
    val initialPage: Int = 0,
    val errorMessage: String? = null
) : UiState

sealed interface AlbumPagerIntent : UiIntent {
    data object Retry : AlbumPagerIntent
}

// No one-off events yet (nothing to navigate to or pop a snackbar for) — this is here so
// AlbumPagerViewModel has a concrete UiEffect to declare, matching AlbumDetailEffect's pattern.
sealed interface AlbumPagerEffect : UiEffect
