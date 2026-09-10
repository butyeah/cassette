package com.ruidoespontaneo.cassette.dayinhistory.domain.usecase

import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
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
    fun `returns the repository's success result unchanged`() {
        val albums = listOf(
            Album(
                id = "album-1",
                title = "Title",
                releaseDate = LocalDate.of(1994, 3, 15),
                artistId = null,
                artistName = "Artist"
            )
        )
        val useCase = GetAlbumsByDayUseCase(fakeRepository { _, _ -> Result.success(albums) })

        val result = runBlocking { useCase(month = 3, day = 15) }

        assertTrue(result.isSuccess)
        assertSame(albums, result.getOrNull())
    }

    @Test
    fun `returns the repository's failure unchanged`() {
        val error = IllegalStateException("boom")
        val useCase = GetAlbumsByDayUseCase(fakeRepository { _, _ -> Result.failure(error) })

        val result = runBlocking { useCase(month = 3, day = 15) }

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun fakeRepository(
        getAlbumsByDay: suspend (month: Int, day: Int) -> Result<List<Album>>
    ) = object : DayInHistoryRepository {
        override suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>> =
            getAlbumsByDay(month, day)
    }
}
