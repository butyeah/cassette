package com.ruidoespontaneo.cassette.nowplaying.presentation

import com.ruidoespontaneo.cassette.albumdetail.preview.FakePreviewPlayer
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayback
import com.ruidoespontaneo.cassette.albumdetail.preview.testPreviewQueue
import com.ruidoespontaneo.cassette.lyrics.domain.LyricsRepository
import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics
import com.ruidoespontaneo.cassette.lyrics.domain.usecase.GetTrackLyricsUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
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
class NowPlayingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val player = FakePreviewPlayer()
    private val queue by lazy { testPreviewQueue(player, TestScope(dispatcher)) }

    private val album = AlbumDetail(
        id = "album-1",
        title = "Title",
        artistName = "Artist",
        primaryType = "Album",
        firstReleaseDate = LocalDate(2001, 6, 17),
        genres = emptyList(),
        ratingValue = null,
        ratingVotesCount = 0,
        tracks = listOf(Track(position = 1, title = "One", lengthMs = 200_000))
    )
    private val previews = mapOf(1 to "https://p/one")

    /** Lyrics by track title; a title that isn't here has none, and "offline" fails. */
    private var lyricsByTrack = mapOf<String, Lyrics>("One" to Lyrics.Plain("one's words"))
    private val lyricsLookups = mutableListOf<String>()

    private fun viewModel() = NowPlayingViewModel(
        queue,
        player,
        GetTrackLyricsUseCase(
            object : LyricsRepository {
                override suspend fun getLyrics(artist: String, track: String, album: String, durationSeconds: Int?): Result<Lyrics?> {
                    lyricsLookups += track
                    return if (track == "offline") Result.failure(Exception("offline")) else Result.success(lyricsByTrack[track])
                }
            }
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `nothing is playing at first`() {
        val viewModel = viewModel()
        idle()

        assertNull(viewModel.state.value.nowPlaying)
        assertFalse(viewModel.state.value.isActive)
    }

    @Test
    fun `mirrors the queue's track and the player's status`() {
        val viewModel = viewModel()
        queue.play(album, previews, 1, listOf("album-1"))
        idle()

        assertEquals("One", viewModel.state.value.nowPlaying?.trackTitle)
        assertEquals(NowPlayingStatus.Loading, viewModel.state.value.status)
        assertTrue(viewModel.state.value.isActive)

        player.setStatus(PreviewPlayback.Status.Playing)
        idle()

        assertEquals(NowPlayingStatus.Playing, viewModel.state.value.status)
    }

    @Test
    fun `Stop keeps the track so Replay can play it again`() {
        val viewModel = viewModel()
        queue.play(album, previews, 1, listOf("album-1"))
        idle()

        viewModel.onIntent(NowPlayingIntent.Stop)
        idle()

        assertEquals(NowPlayingStatus.Stopped, viewModel.state.value.status)
        assertEquals("One", viewModel.state.value.nowPlaying?.trackTitle)

        viewModel.onIntent(NowPlayingIntent.Replay)
        idle()

        assertEquals(listOf("https://p/one", "https://p/one"), player.played)
        assertTrue(viewModel.state.value.isActive)
    }

    @Test
    fun `Next and Previous move through the album`() {
        val twoTracks = album.copy(tracks = album.tracks + Track(position = 2, title = "Two", lengthMs = 200_000))
        val viewModel = viewModel()
        queue.play(twoTracks, mapOf(1 to "https://p/one", 2 to "https://p/two"), 1, listOf("album-1"))
        idle()

        viewModel.onIntent(NowPlayingIntent.Next)
        idle()
        assertEquals("Two", viewModel.state.value.nowPlaying?.trackTitle)

        viewModel.onIntent(NowPlayingIntent.Previous)
        idle()
        assertEquals("One", viewModel.state.value.nowPlaying?.trackTitle)
    }

    @Test
    fun `the playing track's lyrics follow it, looked up once per track`() {
        val twoTracks = album.copy(tracks = album.tracks + Track(position = 2, title = "Two", lengthMs = 200_000))
        lyricsByTrack = mapOf("One" to Lyrics.Plain("one's words"), "Two" to Lyrics.Instrumental)
        val viewModel = viewModel()
        queue.play(twoTracks, mapOf(1 to "https://p/one", 2 to "https://p/two"), 1, listOf("album-1"))
        idle()

        assertEquals(LyricsUiState.Found("one's words"), viewModel.state.value.lyrics)

        player.setStatus(PreviewPlayback.Status.Playing)
        viewModel.onIntent(NowPlayingIntent.Next)
        idle()

        assertEquals(LyricsUiState.Instrumental, viewModel.state.value.lyrics)
        assertEquals(listOf("One", "Two"), lyricsLookups)
    }

    @Test
    fun `a track with no lyrics is NotFound, and a failed lookup is Failed`() {
        val tracks = album.copy(
            tracks = listOf(Track(position = 1, title = "Unknown", lengthMs = null), Track(position = 2, title = "offline", lengthMs = null))
        )
        val viewModel = viewModel()
        queue.play(tracks, mapOf(1 to "https://p/1", 2 to "https://p/2"), 1, listOf("album-1"))
        idle()

        assertEquals(LyricsUiState.NotFound, viewModel.state.value.lyrics)

        viewModel.onIntent(NowPlayingIntent.Next)
        idle()

        assertEquals(LyricsUiState.Failed, viewModel.state.value.lyrics)
    }
}
