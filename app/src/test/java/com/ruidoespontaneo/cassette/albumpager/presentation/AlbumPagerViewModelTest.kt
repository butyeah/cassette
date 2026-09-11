package com.ruidoespontaneo.cassette.albumpager.presentation

import androidx.lifecycle.SavedStateHandle
import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.dayinhistory.domain.usecase.GetAlbumsByDayUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.LocalDate
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
class AlbumPagerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val early = album(id = "early", year = 1994)
    private val late = album(id = "late", year = 2001)
    private val sameYearAsLate = album(id = "same-year-as-late", year = 2001)

    @Test
    fun `flattens the day's albums, newest year first, on init`() {
        val viewModel = viewModel(initialAlbumId = "late") { _, _ ->
            Result.success(listOf(early, late, sameYearAsLate))
        }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf("late", "same-year-as-late", "early"), state.albumIds)
        assertNull(state.errorMessage)
    }

    @Test
    fun `initialPage points at the tapped album`() {
        val viewModel = viewModel(initialAlbumId = "early") { _, _ ->
            Result.success(listOf(early, late, sameYearAsLate))
        }

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.initialPage)
    }

    @Test
    fun `falls back to the first page if the tapped album isn't in the day's list`() {
        val viewModel = viewModel(initialAlbumId = "not-in-this-day") { _, _ ->
            Result.success(listOf(early, late))
        }

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.state.value.initialPage)
    }

    @Test
    fun `a failed load surfaces an error message`() {
        val viewModel = viewModel(initialAlbumId = "late") { _, _ -> Result.failure(IllegalStateException("boom")) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.albumIds.isEmpty())
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `Retry reloads the day's albums`() {
        var calls = 0
        val viewModel = viewModel(initialAlbumId = "late") { _, _ ->
            calls++
            Result.success(listOf(early, late))
        }
        dispatcher.scheduler.advanceUntilIdle()
        val callsAfterInit = calls

        viewModel.onIntent(AlbumPagerIntent.Retry)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(callsAfterInit + 1, calls)
        assertNull(viewModel.state.value.errorMessage)
    }

    private fun album(id: String, year: Int) = Album(
        id = id,
        title = "Title",
        releaseDate = LocalDate.of(year, 6, 17),
        artistId = null,
        artistName = "Artist"
    )

    private fun viewModel(
        initialAlbumId: String,
        getAlbumsByDay: suspend (month: Int, day: Int) -> Result<List<Album>>
    ): AlbumPagerViewModel {
        val repository = object : DayInHistoryRepository {
            override suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>> =
                getAlbumsByDay(month, day)
        }
        val savedStateHandle = SavedStateHandle(
            mapOf(
                ALBUM_PAGER_ARG_MONTH to 6,
                ALBUM_PAGER_ARG_DAY to 17,
                ALBUM_PAGER_ARG_ALBUM_ID to initialAlbumId
            )
        )
        return AlbumPagerViewModel(savedStateHandle, GetAlbumsByDayUseCase(repository))
    }
}
