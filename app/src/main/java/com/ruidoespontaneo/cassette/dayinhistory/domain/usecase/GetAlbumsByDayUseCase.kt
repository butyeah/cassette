package com.ruidoespontaneo.cassette.dayinhistory.domain.usecase

import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import javax.inject.Inject

/**
 * Albums released on [day] of [month], across every year — see
 * [DayInHistoryRepository.getAlbumsByDay] for why this can't just be a MusicBrainz query.
 *
 * Grouped by release year, newest year first, for a "one day like today" screen where each
 * year gets its own card. Albums without a full release date can't be placed in a year and
 * are left out rather than grouped arbitrarily.
 */
class GetAlbumsByDayUseCase @Inject constructor(
    private val repository: DayInHistoryRepository
) {
    suspend operator fun invoke(month: Int, day: Int): Result<List<AlbumsByYear>> =
        repository.getAlbumsByDay(month, day).map { albums ->
            albums
                .filter { it.releaseDate != null }
                .groupBy { it.releaseDate!!.year }
                .map { (year, albumsInYear) -> AlbumsByYear(year, albumsInYear) }
                .sortedByDescending { it.year }
        }
}
