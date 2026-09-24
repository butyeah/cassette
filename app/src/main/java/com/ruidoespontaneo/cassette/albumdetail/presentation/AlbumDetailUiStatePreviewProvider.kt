package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import kotlinx.datetime.LocalDate

private val previewAlbum = AlbumDetail(
    id = "album-1",
    title = "Origin of Symmetry",
    artistName = "Muse",
    primaryType = "Album",
    firstReleaseDate = LocalDate(2001, 6, 17),
    genres = listOf("Alternative Rock", "Space Rock"),
    ratingValue = 4.3,
    ratingVotesCount = 128,
    tracks = listOf(
        Track(position = 1, title = "New Born", lengthMs = 411_000),
        Track(position = 2, title = "Bliss", lengthMs = 284_000),
        Track(position = 3, title = "Space Dementia", lengthMs = 344_000),
        // No recorded length, to exercise the row without a duration.
        Track(position = 4, title = "Hyper Music", lengthMs = null)
    ),
    streamingLinks = StreamingLinks(
        spotify = "https://open.spotify.com/album/preview",
        appleMusic = "https://music.apple.com/us/album/preview"
        // No YouTube Music link, to exercise the row with only some services present.
    )
)

class AlbumDetailUiStatePreviewProvider : PreviewParameterProvider<AlbumDetailUiState> {
    override val values = sequenceOf(
        AlbumDetailUiState(isLoading = false, album = previewAlbum),
        // Previews for all but the last track: one playing, one buffering would need two states,
        // so this shows "playing" on track 2 with play buttons on 1 and 3 and none on track 4.
        AlbumDetailUiState(
            isLoading = false,
            album = previewAlbum,
            previews = mapOf(1 to "https://p/1", 2 to "https://p/2", 3 to "https://p/3"),
            previewPlayback = TrackPlayback(position = 2, isLoading = false, remainingMs = 24_000)
        ),
        AlbumDetailUiState(
            isLoading = false,
            album = previewAlbum,
            previews = mapOf(1 to "https://p/1", 2 to "https://p/2", 3 to "https://p/3"),
            previewPlayback = TrackPlayback(position = 1, isLoading = true)
        ),
        AlbumDetailUiState(
            isLoading = false,
            album = previewAlbum.copy(genres = emptyList(), ratingValue = null, tracks = emptyList())
        ),
        AlbumDetailUiState(isLoading = true),
        AlbumDetailUiState(isLoading = false, errorRes = R.string.error_load_album)
    )
}
