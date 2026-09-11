package com.ruidoespontaneo.cassette.albumdetail.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
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
class AlbumDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val album = AlbumDetail(
        id = "album-1",
        title = "Title",
        artistName = "Artist",
        primaryType = "Album",
        firstReleaseDate = LocalDate.of(2001, 6, 17),
        genres = listOf("Rock"),
        ratingValue = 4.0,
        ratingVotesCount = 10,
        tracks = listOf(Track(position = 1, title = "Track One", lengthMs = 200_000))
    )

    @Test
    fun `loads the album for the given id on init`() {
        var requestedId: String? = null
        val viewModel = viewModel("album-1") { id ->
            requestedId = id
            Result.success(album)
        }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(album, state.album)
        assertNull(state.errorMessage)
        assertEquals("album-1", requestedId)
    }

    @Test
    fun `a failed load surfaces an error message`() {
        val viewModel = viewModel("album-1") { Result.failure(IllegalStateException("boom")) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.album)
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `Retry reloads the album`() {
        var calls = 0
        val viewModel = viewModel("album-1") {
            calls++
            Result.success(album)
        }
        dispatcher.scheduler.advanceUntilIdle()
        val callsAfterInit = calls

        viewModel.onIntent(AlbumDetailIntent.Retry)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(callsAfterInit + 1, calls)
        assertTrue(viewModel.state.value.errorMessage == null)
    }

    private fun viewModel(
        albumId: String,
        getAlbumDetail: suspend (id: String) -> Result<AlbumDetail>
    ): AlbumDetailViewModel {
        val repository = object : MusicBrainzRepository {
            override suspend fun getAlbumsByDate(
                from: LocalDate,
                to: LocalDate,
                limit: Int,
                offset: Int
            ): Result<List<Album>> = error("not used by this test")

            override suspend fun getAlbumDetail(id: String): Result<AlbumDetail> = getAlbumDetail(id)

            // GetAlbumDetailUseCase's live-fallback path always calls this too; returning the
            // fixture's own tracks keeps `state.album` equal to `album` in the success tests.
            override suspend fun getAlbumTracks(id: String): Result<List<Track>> = Result.success(album.tracks)
        }
        // Always a cache miss, so GetAlbumDetailUseCase.invoke() exercises the live
        // getAlbumDetail/getAlbumTracks lambdas this test actually cares about — the cache-hit
        // path has its own dedicated coverage in GetAlbumDetailUseCaseTest.
        val albumTracksRepository = object : AlbumTracksRepository {
            override suspend fun getCachedAlbumDetail(id: String): Result<AlbumDetail?> = Result.success(null)
        }
        return AlbumDetailViewModel(albumId, GetAlbumDetailUseCase(repository, albumTracksRepository))
    }
}
