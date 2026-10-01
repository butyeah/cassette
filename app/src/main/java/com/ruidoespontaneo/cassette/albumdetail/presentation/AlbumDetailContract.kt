package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.annotation.StringRes
import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val album: AlbumDetail? = null,
    @StringRes val errorRes: Int? = null,
    /**
     * 30-second preview URL per [com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track.position].
     * Loaded after [album] and never blocks it — stays empty when iTunes has nothing or the lookup
     * failed, and tracks absent from it simply get no play button.
     */
    val previews: Map<Int, String> = emptyMap(),
    /** The track of this album whose preview is buffering or playing, if any. */
    val previewPlayback: TrackPlayback? = null,
    /**
     * What Wikidata knows about the album, for the "About this album" card. Like [previews], loaded
     * after [album] and never blocking it; stays `null` when there's nothing to show or the lookup
     * failed, and the card is then left out.
     */
    val facts: AlbumFacts? = null
) : UiState

/** [remainingMs] is `null` until the clip's duration is known (always while it's still buffering). */
data class TrackPlayback(val position: Int, val isLoading: Boolean, val remainingMs: Long? = null)

sealed interface AlbumDetailIntent : UiIntent {
    data object Retry : AlbumDetailIntent

    /** Plays the track's preview, or stops it if that track is already the one playing. */
    data class TogglePreview(val trackPosition: Int) : AlbumDetailIntent
}

// No one-off events (autoplay moved to PreviewQueue) — this is here so AlbumDetailViewModel has a
// concrete UiEffect to declare.
sealed interface AlbumDetailEffect : UiEffect
