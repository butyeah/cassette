package com.ruidoespontaneo.cassette.dayinhistory.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.MonthDay

data class OneDayLikeTodayUiState(
    val day: MonthDay = MonthDay.now(),
    val isLoading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val errorMessage: String? = null
) : UiState

sealed interface OneDayLikeTodayIntent : UiIntent {
    data object Retry : OneDayLikeTodayIntent
}

// No one-off events yet (nothing to navigate to or pop a snackbar for) —
// this is here so OneDayLikeTodayViewModel has a concrete UiEffect to declare.
sealed interface OneDayLikeTodayEffect : UiEffect
