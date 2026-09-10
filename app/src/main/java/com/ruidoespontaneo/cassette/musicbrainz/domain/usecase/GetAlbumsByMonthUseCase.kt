package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.YearMonth
import javax.inject.Inject

/**
 * Albums (MusicBrainz releases) released during [month], for a calendar view.
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
}
