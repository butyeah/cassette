package com.ruidoespontaneo.cassette.calendar.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumsByMonthUseCase
import java.time.LocalDate
import java.time.YearMonth
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
class CalendarViewModelTest {

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
    fun `loads the current month on init`() {
        val album = Album(
            id = "album-1",
            title = "Title",
            releaseDate = YearMonth.now().atDay(1),
            artistId = "artist-1",
            artistName = "Artist"
        )
        val viewModel = viewModel { _, _, _, _ -> Result.success(listOf(album)) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf(album), state.albums)
        assertNull(state.errorMessage)
        assertEquals(YearMonth.now(), state.month)
    }

    @Test
    fun `a failed load surfaces an error message`() {
        val viewModel = viewModel { _, _, _, _ -> Result.failure(IllegalStateException("boom")) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.albums.isEmpty())
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `NextMonth and PreviousMonth reload the adjacent month`() {
        var requestedMonth: YearMonth? = null
        val viewModel = viewModel { from, _, _, _ ->
            requestedMonth = YearMonth.from(from)
            Result.success(emptyList())
        }
        dispatcher.scheduler.advanceUntilIdle()
        val initialMonth = viewModel.state.value.month

        viewModel.onIntent(CalendarIntent.NextMonth)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(initialMonth.plusMonths(1), requestedMonth)
        assertEquals(initialMonth.plusMonths(1), viewModel.state.value.month)

        viewModel.onIntent(CalendarIntent.PreviousMonth)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(initialMonth, requestedMonth)
        assertEquals(initialMonth, viewModel.state.value.month)
    }

    @Test
    fun `Retry reloads the current month`() {
        var calls = 0
        val viewModel = viewModel { _, _, _, _ ->
            calls++
            Result.success(emptyList())
        }
        dispatcher.scheduler.advanceUntilIdle()
        val callsAfterInit = calls

        viewModel.onIntent(CalendarIntent.Retry)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(callsAfterInit + 1, calls)
    }

    private fun viewModel(
        getAlbumsByDate: suspend (
            from: LocalDate,
            to: LocalDate,
            limit: Int,
            offset: Int
        ) -> Result<List<Album>>
    ): CalendarViewModel {
        val repository = object : MusicBrainzRepository {
            override suspend fun getAlbumsByDate(
                from: LocalDate,
                to: LocalDate,
                limit: Int,
                offset: Int
            ): Result<List<Album>> = getAlbumsByDate(from, to, limit, offset)
        }
        return CalendarViewModel(GetAlbumsByMonthUseCase(repository))
    }
}
