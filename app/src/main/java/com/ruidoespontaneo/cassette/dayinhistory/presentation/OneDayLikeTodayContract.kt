package com.ruidoespontaneo.cassette.dayinhistory.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.MonthDay

data class OneDayLikeTodayUiState(
    val day: MonthDay = MonthDay.now(),
    val isLoading: Boolean = true,
    val albumsByYear: List<AlbumsByYear> = emptyList(),
    val errorMessage: String? = null,
    /** How the albums are shown. Kept for the session only; changing day doesn't reset it. */
    val layout: AlbumsLayout = AlbumsLayout.List
) : UiState {
    /** Every album of the day in list order, the same order AlbumPagerViewModel pages through. */
    val albums: List<Album> get() = albumsByYear.flatMap { it.albums }
}

enum class AlbumsLayout {
    /** Grouped by release year, with title and artist. */
    List,

    /** Covers only, four per row. */
    Grid
}

sealed interface OneDayLikeTodayIntent : UiIntent {
    data object Retry : OneDayLikeTodayIntent
    data object NextDay : OneDayLikeTodayIntent
    data object PreviousDay : OneDayLikeTodayIntent
    data class SelectDate(val day: MonthDay) : OneDayLikeTodayIntent

    /** Switches between [AlbumsLayout.List] and [AlbumsLayout.Grid]. */
    data object ToggleLayout : OneDayLikeTodayIntent
}

// No one-off events yet (nothing to navigate to or pop a snackbar for) —
// this is here so OneDayLikeTodayViewModel has a concrete UiEffect to declare.
sealed interface OneDayLikeTodayEffect : UiEffect
