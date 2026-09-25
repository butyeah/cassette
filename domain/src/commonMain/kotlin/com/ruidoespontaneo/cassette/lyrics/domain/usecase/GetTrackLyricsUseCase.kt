package com.ruidoespontaneo.cassette.lyrics.domain.usecase

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.lyrics.domain.LyricsRepository
import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

/**
 * Lyrics for the track at [position] on [album]; `null` when there are none, or when [position]
 * isn't one of its tracks.
 */
class GetTrackLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(album: AlbumDetail, position: Int): Result<Lyrics?> {
        val track = album.tracks.firstOrNull { it.position == position } ?: return Result.success(null)
        return lyricsRepository.getLyrics(
            artist = album.artistName,
            track = track.title,
            album = album.title,
            // Whole seconds, rounded, as LRCLIB matches them.
            durationSeconds = track.lengthMs?.let { (it + 500) / 1000 }
        )
    }
}
