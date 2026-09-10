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
        }
    }

    private fun loadAlbums(day: MonthDay) {
        setState { copy(day = day, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAlbumsByDayUseCase(day.monthValue, day.dayOfMonth)
                .onSuccess { albums -> setState { copy(isLoading = false, albums = albums) } }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't load albums")
                    }
                }
        }
    }
}
