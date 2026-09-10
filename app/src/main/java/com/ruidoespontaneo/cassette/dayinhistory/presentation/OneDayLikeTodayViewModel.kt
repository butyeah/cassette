package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.dayinhistory.domain.usecase.GetAlbumsByDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.MonthDay
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OneDayLikeTodayViewModel @Inject constructor(
    private val getAlbumsByDayUseCase: GetAlbumsByDayUseCase
) : MviViewModel<OneDayLikeTodayUiState, OneDayLikeTodayIntent, OneDayLikeTodayEffect>(
    OneDayLikeTodayUiState()
) {

    init {
        loadAlbums(currentState.day)
    }

    override fun onIntent(intent: OneDayLikeTodayIntent) {
        when (intent) {
            OneDayLikeTodayIntent.Retry -> loadAlbums(currentState.day)
            OneDayLikeTodayIntent.NextDay -> loadAlbums(currentState.day.plusOneDay())
            OneDayLikeTodayIntent.PreviousDay -> loadAlbums(currentState.day.minusOneDay())
            OneDayLikeTodayIntent.ToggleCalendar ->
                setState { copy(isCalendarExpanded = !isCalendarExpanded) }
            is OneDayLikeTodayIntent.SelectDate -> {
                setState { copy(isCalendarExpanded = false) }
                loadAlbums(intent.day)
            }
        }
    }

    private fun loadAlbums(day: MonthDay) {
        setState { copy(day = day, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAlbumsByDayUseCase(day.monthValue, day.dayOfMonth)
                .onSuccess { groups -> setState { copy(isLoading = false, albumsByYear = groups) } }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't load albums")
                    }
                }
        }
    }
}

// MonthDay has no plusDays/minusDays of its own — round-tripping through a fixed leap year
// keeps Feb 29 a valid intermediate step regardless of what year it's actually being viewed in.
private const val LEAP_YEAR = 2020

private fun MonthDay.plusOneDay(): MonthDay = MonthDay.from(atYear(LEAP_YEAR).plusDays(1))
private fun MonthDay.minusOneDay(): MonthDay = MonthDay.from(atYear(LEAP_YEAR).minusDays(1))
