package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.YearMonth
import javax.inject.Inject

/**
 * Albums (MusicBrainz releases) released during [month], sorted by release day, for a calendar
 * view. Releases without a full release date (only a year or year-month) can't be placed on a
 * specific day, so they're left out rather than sorted arbitrarily.
 */
class GetAlbumsByMonthUseCase @Inject constructor(
    private val repository: MusicBrainzRepository
) {
    suspend operator fun invoke(
        month: YearMonth,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Album>> =
        repository.getAlbumsByDate(month.atDay(1), month.atEndOfMonth(), limit, offset)
            .map { albums ->
                albums.filter { it.releaseDate != null }.sortedBy { it.releaseDate }
            }
}
