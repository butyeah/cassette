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
    val errorMessage: String? = null,
    /** The album [com.ruidoespontaneo.cassette.albumdetail.preview.PreviewQueue] last started a
     *  track of — when autoplay moves on to another of this pager's albums, the pager follows it. */
    val playingAlbumId: String? = null
) : UiState

sealed interface AlbumPagerIntent : UiIntent {
    data object Retry : AlbumPagerIntent

    /** Sent when the user pages away from the album that's playing. */
    data object StopPreview : AlbumPagerIntent
}

// No one-off events yet (nothing to navigate to or pop a snackbar for) — this is here so
// AlbumPagerViewModel has a concrete UiEffect to declare, matching AlbumDetailEffect's pattern.
sealed interface AlbumPagerEffect : UiEffect
