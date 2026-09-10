package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAlbumsByMonthUseCaseTest {

    private val month = YearMonth.of(2024, 2)

    @Test
    fun `queries the repository for the full month, both ends inclusive`() = runBlocking {
        var receivedFrom: LocalDate? = null
        var receivedTo: LocalDate? = null
        var receivedLimit: Int? = null
        var receivedOffset: Int? = null
        val repository = fakeRepository { from, to, limit, offset ->
            receivedFrom = from
            receivedTo = to
            receivedLimit = limit
            receivedOffset = offset
            Result.success(emptyList())
        }
        val useCase = GetAlbumsByMonthUseCase(repository)

        useCase(month, limit = 10, offset = 20)

        // 2024 is a leap year, so February runs through the 29th.
        assertEquals(LocalDate.of(2024, 2, 1), receivedFrom)
        assertEquals(LocalDate.of(2024, 2, 29), receivedTo)
        assertEquals(10, receivedLimit)
        assertEquals(20, receivedOffset)
    }

    @Test
    fun `sorts albums by release day`() = runBlocking {
        val late = album(id = "late", releaseDate = month.atDay(20))
        val early = album(id = "early", releaseDate = month.atDay(3))
        val middle = album(id = "middle", releaseDate = month.atDay(10))
        val useCase = GetAlbumsByMonthUseCase(
            fakeRepository { _, _, _, _ -> Result.success(listOf(late, early, middle)) }
        )

        val result = useCase(month)

        assertEquals(listOf(early, middle, late), result.getOrNull())
    }

    @Test
    fun `ignores albums without a full release date`() = runBlocking {
        val dated = album(id = "dated", releaseDate = month.atDay(15))
        val undated = album(id = "undated", releaseDate = null)
        val useCase = GetAlbumsByMonthUseCase(
            fakeRepository { _, _, _, _ -> Result.success(listOf(undated, dated)) }
        )

        val result = useCase(month)

        assertEquals(listOf(dated), result.getOrNull())
    }

    private fun album(id: String, releaseDate: LocalDate?) = Album(
        id = id,
        title = "Title",
        releaseDate = releaseDate,
        artistId = "artist-1",
        artistName = "Artist"
    )

    @Test
    fun `returns the repository's failure unchanged`() = runBlocking {
        val error = IllegalStateException("boom")
        val useCase = GetAlbumsByMonthUseCase(fakeRepository { _, _, _, _ -> Result.failure(error) })

        val result = useCase(month)

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun fakeRepository(
        getAlbumsByDate: suspend (
            from: LocalDate,
            to: LocalDate,
            limit: Int,
            offset: Int
        ) -> Result<List<Album>>
    ) = object : MusicBrainzRepository {
        override suspend fun getAlbumsByDate(
            from: LocalDate,
            to: LocalDate,
            limit: Int,
            offset: Int
        ): Result<List<Album>> = getAlbumsByDate(from, to, limit, offset)
    }
}
