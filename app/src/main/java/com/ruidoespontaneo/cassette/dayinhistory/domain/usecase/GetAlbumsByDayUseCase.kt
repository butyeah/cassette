package com.ruidoespontaneo.cassette.dayinhistory.domain.usecase

import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import javax.inject.Inject

/**
 * Albums released on [day] of [month], across every year — see
 * [DayInHistoryRepository.getAlbumsByDay] for why this can't just be a MusicBrainz query.
 */
class GetAlbumsByDayUseCase @Inject constructor(
    private val repository: DayInHistoryRepository
) {
    suspend operator fun invoke(month: Int, day: Int): Result<List<Album>> =
        repository.getAlbumsByDay(month, day)
}
