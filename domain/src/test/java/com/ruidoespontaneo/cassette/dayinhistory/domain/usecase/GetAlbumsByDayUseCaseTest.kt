package com.ruidoespontaneo.cassette.dayinhistory.domain.usecase

import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAlbumsByDayUseCaseTest {

    @Test
    fun `forwards its arguments to the repository`() = runBlocking {
        var receivedMonth: Int? = null
        var receivedDay: Int? = null
        val repository = fakeRepository { month, day ->
            receivedMonth = month
            receivedDay = day
            Result.success(emptyList())
        }
        val useCase = GetAlbumsByDayUseCase(repository)

        useCase(month = 3, day = 15)

        assertEquals(3, receivedMonth)
        assertEquals(15, receivedDay)
    }

    @Test
    fun `groups albums by release year, newest year first`() = runBlocking {
        val early = album(id = "early", year = 1994)
        val late = album(id = "late", year = 2001)
        val sameYearAsLate = album(id = "same-year-as-late", year = 2001)
        val useCase = GetAlbumsByDayUseCase(
            fakeRepository { _, _ -> Result.success(listOf(early, late, sameYearAsLate)) }
        )

        val result = useCase(month = 6, day = 17)

        assertEquals(
            listOf(
                AlbumsByYear(2001, listOf(late, sameYearAsLate)),
                AlbumsByYear(1994, listOf(early))
            ),
            result.getOrNull()
        )
    }

    @Test
    fun `ignores albums without a full release date`() = runBlocking {
        val dated = album(id = "dated", year = 1994)
        val undated = dated.copy(id = "undated", releaseDate = null)
        val useCase = GetAlbumsByDayUseCase(
            fakeRepository { _, _ -> Result.success(listOf(undated, dated)) }
        )

        val result = useCase(month = 6, day = 17)

        assertEquals(listOf(AlbumsByYear(1994, listOf(dated))), result.getOrNull())
    }

    @Test
    fun `returns the repository's failure unchanged`() {
        val error = IllegalStateException("boom")
        val useCase = GetAlbumsByDayUseCase(fakeRepository { _, _ -> Result.failure(error) })

        val result = runBlocking { useCase(month = 3, day = 15) }

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun album(id: String, year: Int) = Album(
        id = id,
        title = "Title",
        releaseDate = LocalDate.of(year, 6, 17),
        artistId = null,
        artistName = "Artist"
    )

    private fun fakeRepository(
        getAlbumsByDay: suspend (month: Int, day: Int) -> Result<List<Album>>
    ) = object : DayInHistoryRepository {
        override suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>> =
            getAlbumsByDay(month, day)
    }
}
