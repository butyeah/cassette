package com.ruidoespontaneo.cassette.albumdetail.preview

import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewQueueTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val player = FakePreviewPlayer()

    private val first = album("first", "One", "Two", "Three")
    private val empty = album("empty", "Silence")
    private val second = album("second", "Four", "Five")
    private val dayAlbumIds = listOf("first", "empty", "missing", "second")

    // Every track of every album has a preview, except "empty"'s and "first"'s track 2.
    private val queue = testPreviewQueue(
        player,
        scope,
        albums = listOf(first, empty, second).associateBy { it.id },
        previews = { album ->
            Result.success(
                if (album.id == "empty") emptyList()
                else album.tracks.filterNot { album.id == "first" && it.position == 2 }.map { TrackPreview(it.title, urlOf(album, it.position)) }
            )
        }
    )

    private val firstPreviews = mapOf(1 to urlOf(first, 1), 3 to urlOf(first, 3))

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `play starts the track and records what's playing`() {
        queue.play(first, firstPreviews, 1, dayAlbumIds)

        assertEquals(listOf(urlOf(first, 1)), player.played)
        assertEquals(NowPlaying(first, firstPreviews, 1, dayAlbumIds), queue.nowPlaying.value)
        assertEquals("One", queue.nowPlaying.value?.trackTitle)
    }

    @Test
    fun `a finished clip moves on to the album's next previewable track`() {
        idle()
        queue.play(first, firstPreviews, 1, dayAlbumIds)

        player.complete()
        idle()

        // Track 2 has no preview, so it's skipped.
        assertEquals(listOf(urlOf(first, 1), urlOf(first, 3)), player.played)
        assertEquals(3, queue.nowPlaying.value?.position)
    }

    @Test
    fun `after the album's last track it moves on to the next album with previews`() {
        idle()
        queue.play(first, firstPreviews, 3, dayAlbumIds)

        player.complete()
        idle()

        // "empty" has nothing to preview and "missing" fails to load, so both are skipped.
        assertEquals(urlOf(second, 1), player.played.last())
        assertEquals("second", queue.nowPlaying.value?.album?.id)
        assertEquals(1, queue.nowPlaying.value?.position)
        assertFalse(queue.isAdvancing.value)
    }

    @Test
    fun `it stops after the day's last album`() {
        idle()
        queue.play(second, mapOf(1 to urlOf(second, 1), 2 to urlOf(second, 2)), 2, dayAlbumIds)

        player.complete()
        idle()

        assertEquals(listOf(urlOf(second, 2)), player.played)
        assertNull(player.playback.value)
    }

    @Test
    fun `a stopped clip doesn't advance`() {
        idle()
        queue.play(first, firstPreviews, 1, dayAlbumIds)

        queue.stop()
        idle()

        assertEquals(listOf(urlOf(first, 1)), player.played)
        assertNull(player.playback.value)
    }

    @Test
    fun `stop keeps what was playing and replay starts it again`() {
        idle()
        queue.play(first, firstPreviews, 3, dayAlbumIds)
        queue.stop()

        assertEquals(3, queue.nowPlaying.value?.position)

        queue.replay()

        assertEquals(listOf(urlOf(first, 3), urlOf(first, 3)), player.played)
    }

    @Test
    fun `a clip that isn't the queue's doesn't advance it`() {
        idle()
        queue.play(first, firstPreviews, 1, dayAlbumIds)
        player.play("https://p/elsewhere")

        player.complete()
        idle()

        assertEquals(listOf(urlOf(first, 1), "https://p/elsewhere"), player.played)
    }

    @Test
    fun `stopping just as a clip ends doesn't move on`() {
        idle()
        queue.play(first, firstPreviews, 3, dayAlbumIds)
        player.complete()

        queue.stop()
        idle()

        assertEquals(listOf(urlOf(first, 3)), player.played)
        assertFalse(queue.isAdvancing.value)
    }

    @Test
    fun `stopping while the next album is being looked up cancels it`() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val gatedQueue = testPreviewQueue(
            player,
            scope,
            albums = mapOf("first" to first, "second" to second),
            previews = { album ->
                if (album.id == "second") gate.await()
                Result.success(album.tracks.map { TrackPreview(it.title, urlOf(album, it.position)) })
            }
        )
        idle()
        gatedQueue.play(first, firstPreviews, 3, listOf("first", "second"))
        player.complete()
        idle()
        assertTrue(gatedQueue.isAdvancing.value)

        gatedQueue.stop()
        gate.complete(Unit)
        idle()

        assertEquals(listOf(urlOf(first, 3)), player.played)
        assertFalse(gatedQueue.isAdvancing.value)
    }

    private val secondPreviews get() = mapOf(1 to urlOf(second, 1), 2 to urlOf(second, 2))

    @Test
    fun `next skips to the album's next previewable track`() {
        idle()
        queue.play(first, firstPreviews, 1, dayAlbumIds)

        queue.skipToNext()

        assertEquals(urlOf(first, 3), player.played.last())
    }

    @Test
    fun `next at the album's end moves to the next album's first preview`() {
        idle()
        queue.play(first, firstPreviews, 3, dayAlbumIds)

        queue.skipToNext()
        idle()

        assertEquals(urlOf(second, 1), player.played.last())
        assertEquals("second", queue.nowPlaying.value?.album?.id)
    }

    @Test
    fun `next with no later album that has previews stops`() {
        idle()
        queue.play(first, firstPreviews, 3, listOf("first", "empty"))

        queue.skipToNext()
        idle()

        assertEquals(listOf(urlOf(first, 3)), player.played)
        assertNull(player.playback.value)
    }

    @Test
    fun `previous goes back within the album`() {
        idle()
        queue.play(first, firstPreviews, 3, dayAlbumIds)

        queue.skipToPrevious()

        // Track 2 has no preview, so it's skipped.
        assertEquals(urlOf(first, 1), player.played.last())
    }

    @Test
    fun `previous at the album's start plays the previous album's last preview`() {
        idle()
        queue.play(second, secondPreviews, 1, dayAlbumIds)

        queue.skipToPrevious()
        idle()

        // "missing" fails to load and "empty" has no previews, so it lands on "first", at its last.
        assertEquals(urlOf(first, 3), player.played.last())
        assertEquals("first", queue.nowPlaying.value?.album?.id)
        assertEquals(3, queue.nowPlaying.value?.position)
    }

    @Test
    fun `previous at the day's first track changes nothing`() {
        idle()
        queue.play(first, firstPreviews, 1, dayAlbumIds)

        queue.skipToPrevious()
        idle()

        assertEquals(listOf(urlOf(first, 1)), player.played)
        assertEquals(1, queue.nowPlaying.value?.position)
    }

    @Test
    fun `skipping while stopped plays`() {
        idle()
        queue.play(first, firstPreviews, 1, dayAlbumIds)
        queue.stop()

        queue.skipToNext()

        assertEquals(urlOf(first, 3), player.played.last())
        assertTrue(player.playback.value != null)
    }

    @Test
    fun `hasNext and hasPrevious reflect the day's edges`() {
        val atStart = NowPlaying(first, firstPreviews, 1, dayAlbumIds)
        assertFalse(atStart.hasPrevious)
        assertTrue(atStart.hasNext)

        val atEnd = NowPlaying(second, secondPreviews, 2, dayAlbumIds)
        assertTrue(atEnd.hasPrevious)
        assertFalse(atEnd.hasNext)

        val alone = NowPlaying(first, mapOf(1 to urlOf(first, 1)), 1, listOf("first"))
        assertFalse(alone.hasPrevious)
        assertFalse(alone.hasNext)
    }

    private fun album(id: String, vararg titles: String) = AlbumDetail(
        id = id,
        title = "Album $id",
        artistName = "Artist",
        primaryType = "Album",
        firstReleaseDate = LocalDate.of(2001, 6, 17),
        genres = emptyList(),
        ratingValue = null,
        ratingVotesCount = 0,
        tracks = titles.mapIndexed { index, title -> Track(position = index + 1, title = title, lengthMs = 200_000) }
    )

    private fun urlOf(album: AlbumDetail, position: Int) = "https://p/${album.id}/$position"
}
