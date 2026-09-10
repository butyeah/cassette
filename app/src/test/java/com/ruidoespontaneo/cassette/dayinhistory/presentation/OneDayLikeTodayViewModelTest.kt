package com.ruidoespontaneo.cassette.dayinhistory.presentation

import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.dayinhistory.domain.usecase.GetAlbumsByDayUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.LocalDate
import java.time.MonthDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OneDayLikeTodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads today's day on init`() {
        val today = MonthDay.now()
        val album = Album(
            id = "album-1",
            title = "Title",
            releaseDate = LocalDate.of(1994, today.monthValue, today.dayOfMonth),
            artistId = null,
            artistName = "Artist"
        )
        val viewModel = viewModel { _, _ -> Result.success(listOf(album)) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf(album), state.albums)
        assertNull(state.errorMessage)
        assertEquals(today, state.day)
    }

    @Test
    fun `requests the current month and day from the use case`() {
        var receivedMonth: Int? = null
        var receivedDay: Int? = null
        val viewModel = viewModel { month, day ->
            receivedMonth = month
            receivedDay = day
            Result.success(emptyList())
        }

        dispatcher.scheduler.advanceUntilIdle()

        val today = MonthDay.now()
        assertEquals(today.monthValue, receivedMonth)
        assertEquals(today.dayOfMonth, receivedDay)
    }

    @Test
    fun `a failed load surfaces an error message`() {
        val viewModel = viewModel { _, _ -> Result.failure(IllegalStateException("boom")) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.albums.isEmpty())
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `Retry reloads the current day`() {
        var calls = 0
        val viewModel = viewModel { _, _ ->
            calls++
            Result.success(emptyList())
        }
        dispatcher.scheduler.advanceUntilIdle()
        val callsAfterInit = calls

        viewModel.onIntent(OneDayLikeTodayIntent.Retry)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(callsAfterInit + 1, calls)
    }

    private fun viewModel(
        getAlbumsByDay: suspend (month: Int, day: Int) -> Result<List<Album>>
    ): OneDayLikeTodayViewModel {
        val repository = object : DayInHistoryRepository {
            override suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>> =
                getAlbumsByDay(month, day)
        }
        return OneDayLikeTodayViewModel(GetAlbumsByDayUseCase(repository))
    }
}
