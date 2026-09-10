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
    fun `returns the repository's success result unchanged`() = runBlocking {
        val albums = listOf(
            Album(
                id = "album-1",
                title = "Title",
                releaseDate = month.atDay(1),
                artistId = "artist-1",
                artistName = "Artist"
            )
        )
        val useCase = GetAlbumsByMonthUseCase(fakeRepository { _, _, _, _ -> Result.success(albums) })

        val result = useCase(month)

        assertTrue(result.isSuccess)
        assertSame(albums, result.getOrNull())
    }

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
