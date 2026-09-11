package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import java.time.LocalDate

private val previewAlbum = AlbumDetail(
    id = "album-1",
    title = "Origin of Symmetry",
    artistName = "Muse",
    primaryType = "Album",
    firstReleaseDate = LocalDate.of(2001, 6, 17),
    genres = listOf("Alternative Rock", "Space Rock"),
    ratingValue = 4.3,
    ratingVotesCount = 128
)

class AlbumDetailUiStatePreviewProvider : PreviewParameterProvider<AlbumDetailUiState> {
    override val values = sequenceOf(
        AlbumDetailUiState(isLoading = false, album = previewAlbum),
        AlbumDetailUiState(isLoading = false, album = previewAlbum.copy(genres = emptyList(), ratingValue = null)),
        AlbumDetailUiState(isLoading = true),
        AlbumDetailUiState(isLoading = false, errorMessage = "Couldn't load album")
    )
}
