package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.R
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
            is OneDayLikeTodayIntent.SelectDate -> loadAlbums(intent.day)
            OneDayLikeTodayIntent.ToggleLayout -> setState {
                copy(layout = if (layout == AlbumsLayout.List) AlbumsLayout.Grid else AlbumsLayout.List)
            }
        }
    }

    private fun loadAlbums(day: MonthDay) {
        setState { copy(day = day, isLoading = true, errorRes = null) }
        viewModelScope.launch {
            getAlbumsByDayUseCase(day.monthValue, day.dayOfMonth)
                .onSuccess { groups -> setState { copy(isLoading = false, albumsByYear = groups) } }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorRes = R.string.error_load_albums)
                    }
                }
        }
    }
}
