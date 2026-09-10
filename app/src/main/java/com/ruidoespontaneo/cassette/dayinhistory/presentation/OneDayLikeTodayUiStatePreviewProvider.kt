package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.LocalDate
import java.time.MonthDay

private val previewGroups = listOf(
    AlbumsByYear(
        year = 2001,
        albums = listOf(
            Album(
                id = "1",
                title = "Origin of Symmetry",
                releaseDate = LocalDate.of(2001, 6, 17),
                artistId = null,
                artistName = "Muse"
            )
        )
    ),
    AlbumsByYear(
        year = 1994,
        albums = listOf(
            Album(
                id = "2",
                title = "The Downward Spiral",
                releaseDate = LocalDate.of(1994, 6, 17),
                artistId = null,
                artistName = "Nine Inch Nails"
            ),
            Album(
                id = "3",
                title = "Superunknown",
                releaseDate = LocalDate.of(1994, 6, 17),
                artistId = null,
                artistName = "Soundgarden"
            )
        )
    )
)

class OneDayLikeTodayUiStatePreviewProvider : PreviewParameterProvider<OneDayLikeTodayUiState> {
    private val day = MonthDay.of(6, 17)

    override val values = sequenceOf(
        OneDayLikeTodayUiState(day = day, isLoading = false, albumsByYear = previewGroups),
        OneDayLikeTodayUiState(
            day = day,
            isLoading = false,
            albumsByYear = previewGroups,
            isCalendarExpanded = true
        ),
        OneDayLikeTodayUiState(day = day, isLoading = false),
        OneDayLikeTodayUiState(day = day, isLoading = true),
        OneDayLikeTodayUiState(day = day, isLoading = false, errorMessage = "Couldn't load albums")
    )
}
