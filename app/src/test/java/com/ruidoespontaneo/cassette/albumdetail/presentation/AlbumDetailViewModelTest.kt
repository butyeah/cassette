package com.ruidoespontaneo.cassette.albumdetail.presentation

import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.albumdetail.preview.FakePreviewPlayer
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayback
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewQueue
import com.ruidoespontaneo.cassette.albumdetail.preview.testPreviewQueue
import com.ruidoespontaneo.cassette.itunes.domain.ItunesRepository
import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.itunes.domain.usecase.GetTrackPreviewsUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
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
        firstReleaseDate = LocalDate(2001, 6, 17),
        genres = listOf("Rock"),
        ratingValue = 4.0,
        ratingVotesCount = 10,
        tracks = listOf(
            Track(position = 1, title = "Track One", lengthMs = 200_000),
            Track(position = 2, title = "Track Two", lengthMs = 180_000)
        )
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
        assertNull(state.errorRes)
        assertEquals("album-1", requestedId)
    }

    @Test
    fun `a failed load surfaces an error message`() {
        val viewModel = viewModel("album-1") { Result.failure(IllegalStateException("boom")) }

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.album)
        assertEquals(R.string.error_load_album, state.errorRes)
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
        assertTrue(viewModel.state.value.errorRes == null)
    }

    @Test
    fun `loads previews after the album and keys them by track position`() {
        val viewModel = viewModel("album-1")

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(mapOf(1 to ONE_URL, 2 to TWO_URL), viewModel.state.value.previews)
    }

    @Test
    fun `the album is shown before its previews arrive`() {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel("album-1", getPreviews = { gate.await(); Result.success(previewsFor(it)) })

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(album, viewModel.state.value.album)
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.previews.isEmpty())

        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.previews.size)
    }

    @Test
    fun `a failed preview lookup leaves the album visible with no previews and no error`() {
        val viewModel = viewModel("album-1", getPreviews = { Result.failure(IllegalStateException("offline")) })

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(album, state.album)
        assertNull(state.errorRes)
        assertTrue(state.previews.isEmpty())
    }

    @Test
    fun `TogglePreview plays that track's clip`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(AlbumDetailIntent.TogglePreview(2))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(TWO_URL), player.played)
        assertEquals(TrackPlayback(position = 2, isLoading = true), viewModel.state.value.previewPlayback)
    }

    @Test
    fun `playback reports playing once the player has buffered`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(AlbumDetailIntent.TogglePreview(1))

        player.setStatus(PreviewPlayback.Status.Playing)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TrackPlayback(position = 1, isLoading = false), viewModel.state.value.previewPlayback)
    }

    @Test
    fun `playback carries the clip's remaining time from the player`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(AlbumDetailIntent.TogglePreview(1))
        player.setStatus(PreviewPlayback.Status.Playing)

        player.setRemaining(24_000)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            TrackPlayback(position = 1, isLoading = false, remainingMs = 24_000),
            viewModel.state.value.previewPlayback
        )
    }

    @Test
    fun `TogglePreview on the track that is playing stops it`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(AlbumDetailIntent.TogglePreview(1))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(AlbumDetailIntent.TogglePreview(1))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, player.stopCount)
        assertEquals(listOf(ONE_URL), player.played)
        assertNull(viewModel.state.value.previewPlayback)
    }

    @Test
    fun `TogglePreview on another track switches to it`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(AlbumDetailIntent.TogglePreview(1))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(AlbumDetailIntent.TogglePreview(2))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ONE_URL, TWO_URL), player.played)
        assertEquals(2, viewModel.state.value.previewPlayback?.position)
    }

    @Test
    fun `TogglePreview on a track with no preview does nothing`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(AlbumDetailIntent.TogglePreview(99))

        assertTrue(player.played.isEmpty())
        assertEquals(0, player.stopCount)
    }

    @Test
    fun `ignores playback of a clip that belongs to another album`() {
        val player = FakePreviewPlayer()
        val viewModel = viewModel("album-1", player = player)
        dispatcher.scheduler.advanceUntilIdle()

        player.play("https://p/other-album")
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.previewPlayback)
    }

    @Test
    fun `TogglePreview hands the queue the day's album order`() {
        val player = FakePreviewPlayer()
        val queue = previewQueue(player)
        val viewModel = viewModel("album-1", player = player, queue = queue, dayAlbumIds = listOf("album-1", "album-2"))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(AlbumDetailIntent.TogglePreview(2))

        val nowPlaying = queue.nowPlaying.value
        assertEquals("album-1", nowPlaying?.album?.id)
        assertEquals(2, nowPlaying?.position)
        assertEquals(listOf("album-1", "album-2"), nowPlaying?.dayAlbumIds)
    }

    private fun previewsFor(album: AlbumDetail) = listOf(
        TrackPreview(title = "Track One", url = ONE_URL),
        TrackPreview(title = "Track Two", url = TWO_URL)
    )

    private fun viewModel(
        albumId: String,
        player: FakePreviewPlayer = FakePreviewPlayer(),
        queue: PreviewQueue = previewQueue(player),
        dayAlbumIds: List<String> = listOf(albumId),
        getPreviews: suspend (AlbumDetail) -> Result<List<TrackPreview>> = { Result.success(previewsFor(it)) },
        album: AlbumDetail = this.album,
        getAlbumDetail: suspend (id: String) -> Result<AlbumDetail> = { Result.success(album) }
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
        val itunesRepository = object : ItunesRepository {
            override suspend fun getPreviews(album: AlbumDetail): Result<List<TrackPreview>> = getPreviews(album)
        }
        return AlbumDetailViewModel(
            albumId,
            dayAlbumIds,
            GetAlbumDetailUseCase(repository, albumTracksRepository),
            GetTrackPreviewsUseCase(itunesRepository),
            player,
            queue
        )
    }

    // Autoplay itself is PreviewQueueTest's; here the queue only needs to reach the player.
    private fun previewQueue(player: FakePreviewPlayer) = testPreviewQueue(player, TestScope(dispatcher))

    private companion object {
        const val ONE_URL = "https://p/one"
        const val TWO_URL = "https://p/two"
    }
}
