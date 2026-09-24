package com.ruidoespontaneo.cassette.albumdetail.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val album: AlbumDetail? = null,
    val errorMessage: String? = null,
    /**
     * 30-second preview URL per [com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track.position].
     * Loaded after [album] and never blocks it — stays empty when iTunes has nothing or the lookup
     * failed, and tracks absent from it simply get no play button.
     */
    val previews: Map<Int, String> = emptyMap(),
    /** The track of this album whose preview is buffering or playing, if any. */
    val previewPlayback: TrackPlayback? = null
) : UiState

/** [remainingMs] is `null` until the clip's duration is known (always while it's still buffering). */
data class TrackPlayback(val position: Int, val isLoading: Boolean, val remainingMs: Long? = null)

sealed interface AlbumDetailIntent : UiIntent {
    data object Retry : AlbumDetailIntent

    /** Plays the track's preview, or stops it if that track is already the one playing. */
    data class TogglePreview(val trackPosition: Int) : AlbumDetailIntent

    /**
     * Plays this album's first preview as soon as its previews are known — sent when autoplay
     * arrives here from the previous album. An album with nothing to play sends
     * [AlbumDetailEffect.TracklistFinished] straight away, so autoplay moves on past it.
     */
    data object AutoPlay : AlbumDetailIntent
}

sealed interface AlbumDetailEffect : UiEffect {
    /** Autoplay ran out of this album's previews; the next album should take over. */
    data object TracklistFinished : AlbumDetailEffect
}
