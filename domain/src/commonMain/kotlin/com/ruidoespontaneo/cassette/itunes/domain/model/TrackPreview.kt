package com.ruidoespontaneo.cassette.itunes.domain.model

/**
 * A 30-second iTunes preview clip for one song on an album's iTunes edition. This is iTunes' own
 * view of the song — [title] is iTunes' spelling, not necessarily
 * [com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track.title]'s — so callers match it
 * back to a MusicBrainz track (see
 * [com.ruidoespontaneo.cassette.itunes.domain.usecase.GetTrackPreviewsUseCase]) rather than
 * assuming the two line up.
 */
data class TrackPreview(
    val title: String,
    val url: String
)
