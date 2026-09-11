package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import java.time.LocalDate

private val previewAlbum = AlbumDetail(
    id = "album-1",
    title = "Origin of Symmetry",
    artistName = "Muse",
    primaryType = "Album",
    firstReleaseDate = LocalDate.of(2001, 6, 17),
    genres = listOf("Alternative Rock", "Space Rock"),
    ratingValue = 4.3,
    ratingVotesCount = 128,
    tracks = listOf(
        Track(position = 1, title = "New Born", lengthMs = 411_000),
        Track(position = 2, title = "Bliss", lengthMs = 284_000),
        Track(position = 3, title = "Space Dementia", lengthMs = 344_000),
        // No recorded length, to exercise the row without a duration.
        Track(position = 4, title = "Hyper Music", lengthMs = null)
    )
)

class AlbumDetailUiStatePreviewProvider : PreviewParameterProvider<AlbumDetailUiState> {
    override val values = sequenceOf(
        AlbumDetailUiState(isLoading = false, album = previewAlbum),
        AlbumDetailUiState(
            isLoading = false,
            album = previewAlbum.copy(genres = emptyList(), ratingValue = null, tracks = emptyList())
        ),
        AlbumDetailUiState(isLoading = true),
        AlbumDetailUiState(isLoading = false, errorMessage = "Couldn't load album")
    )
}
