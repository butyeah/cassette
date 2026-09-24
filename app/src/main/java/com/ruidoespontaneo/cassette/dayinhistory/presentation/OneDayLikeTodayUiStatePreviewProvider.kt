package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.MonthDay
import kotlinx.datetime.LocalDate

private val previewGroups = listOf(
    AlbumsByYear(
        year = 2001,
        albums = listOf(
            Album(
                id = "1",
                title = "Origin of Symmetry",
                releaseDate = LocalDate(2001, 6, 17),
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
                releaseDate = LocalDate(1994, 6, 17),
                artistId = null,
                artistName = "Nine Inch Nails"
            ),
            Album(
                id = "3",
                title = "Superunknown",
                releaseDate = LocalDate(1994, 6, 17),
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
        OneDayLikeTodayUiState(day = day, isLoading = false),
        OneDayLikeTodayUiState(day = day, isLoading = true),
        OneDayLikeTodayUiState(day = day, isLoading = false, errorRes = R.string.error_load_albums)
    )
}
