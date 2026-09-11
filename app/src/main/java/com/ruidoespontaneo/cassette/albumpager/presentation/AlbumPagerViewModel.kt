package com.ruidoespontaneo.cassette.albumpager.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.dayinhistory.domain.usecase.GetAlbumsByDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Nav-graph argument names for the day and the initially-tapped album's MBID. */
const val ALBUM_PAGER_ARG_MONTH = "month"
const val ALBUM_PAGER_ARG_DAY = "day"
const val ALBUM_PAGER_ARG_ALBUM_ID = "albumId"

/**
 * The full list of albums released on one day (across every year), in the same order the Daily
 * screen renders them, plus which one to open on — everything AlbumPagerScreen needs to drive its
 * HorizontalPager. Fetching each page's own [com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail]
 * (title, tracklist, ...) stays with [com.ruidoespontaneo.cassette.albumdetail.presentation.AlbumDetailViewModel],
 * one instance per page.
 */
@HiltViewModel
class AlbumPagerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlbumsByDayUseCase: GetAlbumsByDayUseCase
) : MviViewModel<AlbumPagerUiState, AlbumPagerIntent, AlbumPagerEffect>(
    AlbumPagerUiState()
) {

    private val month: Int = checkNotNull(savedStateHandle[ALBUM_PAGER_ARG_MONTH]) {
        "AlbumPagerViewModel requires a non-null $ALBUM_PAGER_ARG_MONTH nav argument"
    }
    private val day: Int = checkNotNull(savedStateHandle[ALBUM_PAGER_ARG_DAY]) {
        "AlbumPagerViewModel requires a non-null $ALBUM_PAGER_ARG_DAY nav argument"
    }
    private val initialAlbumId: String = checkNotNull(savedStateHandle[ALBUM_PAGER_ARG_ALBUM_ID]) {
        "AlbumPagerViewModel requires a non-null $ALBUM_PAGER_ARG_ALBUM_ID nav argument"
    }

    init {
        loadAlbumIds()
    }

    override fun onIntent(intent: AlbumPagerIntent) {
        when (intent) {
            AlbumPagerIntent.Retry -> loadAlbumIds()
        }
    }

    private fun loadAlbumIds() {
        setState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAlbumsByDayUseCase(month, day)
                .onSuccess { groups ->
                    val albumIds = groups.flatMap { it.albums }.map { it.id }
                    setState {
                        copy(
                            isLoading = false,
                            albumIds = albumIds,
                            // Falls back to the first page if the tapped album somehow isn't in
                            // its own day's list (shouldn't happen — the tap is what got us here).
                            initialPage = albumIds.indexOf(initialAlbumId).coerceAtLeast(0)
                        )
                    }
                }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't load albums")
                    }
                }
        }
    }
}
