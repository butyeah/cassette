package com.ruidoespontaneo.cassette.calendar.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumsByMonthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAlbumsByMonthUseCase: GetAlbumsByMonthUseCase
) : MviViewModel<CalendarUiState, CalendarIntent, CalendarEffect>(CalendarUiState()) {

    init {
        loadAlbums(currentState.month)
    }

    override fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.SelectMonth -> loadAlbums(intent.month)
            CalendarIntent.NextMonth -> loadAlbums(currentState.month.plusMonths(1))
            CalendarIntent.PreviousMonth -> loadAlbums(currentState.month.minusMonths(1))
            CalendarIntent.Retry -> loadAlbums(currentState.month)
        }
    }

    private fun loadAlbums(month: YearMonth) {
        setState { copy(month = month, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAlbumsByMonthUseCase(month)
                .onSuccess { albums -> setState { copy(isLoading = false, albums = albums) } }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't load albums")
                    }
                }
        }
    }
}
