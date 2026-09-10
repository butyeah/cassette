package com.ruidoespontaneo.cassette.calendar.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val errorMessage: String? = null
) : UiState

sealed interface CalendarIntent : UiIntent {
    data class SelectMonth(val month: YearMonth) : CalendarIntent
    data object NextMonth : CalendarIntent
    data object PreviousMonth : CalendarIntent
    data object Retry : CalendarIntent
}

// No one-off events yet (nothing to navigate to or pop a snackbar for) —
// this is here so MonthlyViewModel has a concrete UiEffect to declare.
sealed interface CalendarEffect : UiEffect
