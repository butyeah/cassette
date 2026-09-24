package com.ruidoespontaneo.cassette.albumdetail.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewDisplayFormattingTest {

    @Test
    fun `unknown remaining time shows dashes`() {
        assertEquals("--:--", remainingTimeText(null))
    }

    @Test
    fun `whole seconds are shown as is`() {
        assertEquals("00:24", remainingTimeText(24_000))
    }

    @Test
    fun `a partial second rounds up`() {
        assertEquals("00:24", remainingTimeText(23_100))
    }

    @Test
    fun `a finished clip shows zero`() {
        assertEquals("00:00", remainingTimeText(0))
    }

    @Test
    fun `minutes roll over`() {
        assertEquals("01:05", remainingTimeText(65_000))
    }

    private val tracks = listOf(
        Track(position = 1, title = "Track One", lengthMs = 200_000),
        Track(position = 2, title = "Track Two", lengthMs = 180_000)
    )

    @Test
    fun `the preview title is the playing track's`() {
        assertEquals("Track Two", previewTrackTitle(tracks, TrackPlayback(position = 2, isLoading = false)))
    }

    @Test
    fun `there is no preview title while nothing is playing`() {
        assertNull(previewTrackTitle(tracks, null))
    }

    @Test
    fun `there is no preview title for a position the tracklist doesn't have`() {
        assertNull(previewTrackTitle(tracks, TrackPlayback(position = 9, isLoading = true)))
    }

    @Test
    fun `the status is stopped while nothing is playing`() {
        assertEquals(PreviewStatus.Stopped, previewStatus(null))
    }

    @Test
    fun `a buffering clip already counts as playing`() {
        assertEquals(PreviewStatus.Playing, previewStatus(TrackPlayback(position = 1, isLoading = true)))
    }

    @Test
    fun `the status is playing once the clip is audible`() {
        assertEquals(PreviewStatus.Playing, previewStatus(TrackPlayback(position = 1, isLoading = false)))
    }
}
