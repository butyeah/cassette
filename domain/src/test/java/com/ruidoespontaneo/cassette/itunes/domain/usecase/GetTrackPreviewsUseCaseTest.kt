package com.ruidoespontaneo.cassette.itunes.domain.usecase

import com.ruidoespontaneo.cassette.itunes.domain.ItunesRepository
import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTrackPreviewsUseCaseTest {

    private fun album(vararg titles: String) = AlbumDetail(
        id = "album-1",
        title = "Album",
        artistName = "Artist",
        primaryType = "Album",
        firstReleaseDate = LocalDate.of(2001, 6, 17),
        genres = emptyList(),
        ratingValue = null,
        ratingVotesCount = 0,
        tracks = titles.mapIndexed { i, title -> Track(position = i + 1, title = title, lengthMs = null) }
    )

    private fun preview(title: String) = TrackPreview(title = title, url = "https://p/$title")

    private fun useCase(result: Result<List<TrackPreview>>) = GetTrackPreviewsUseCase(
        object : ItunesRepository {
            override suspend fun getPreviews(album: AlbumDetail) = result
        }
    )

    @Test
    fun `matches previews to tracks by title, keyed by track position`() = runBlocking {
        val result = useCase(Result.success(listOf(preview("Two"), preview("One")))).invoke(album("One", "Two"))

        assertEquals(mapOf(1 to "https://p/One", 2 to "https://p/Two"), result.getOrNull())
    }

    @Test
    fun `matches through iTunes decorations`() = runBlocking {
        val result = useCase(Result.success(listOf(preview("Song (feat. Guest) [Remastered]"))))
            .invoke(album("Song"))

        assertEquals(mapOf(1 to "https://p/Song (feat. Guest) [Remastered]"), result.getOrNull())
    }

    @Test
    fun `falls back to index when titles differ but counts agree`() = runBlocking {
        val result = useCase(Result.success(listOf(preview("Alpha"), preview("Beta"))))
            .invoke(album("Alfa", "Bravo"))

        assertEquals(mapOf(1 to "https://p/Alpha", 2 to "https://p/Beta"), result.getOrNull())
    }

    @Test
    fun `does not guess by index when counts differ`() = runBlocking {
        // Bonus tracks make position an unreliable signal, so only the title match survives.
        val result = useCase(Result.success(listOf(preview("One"), preview("Bonus"), preview("Extra"))))
            .invoke(album("One", "Unmatched"))

        assertEquals(mapOf(1 to "https://p/One"), result.getOrNull())
    }

    @Test
    fun `is empty when iTunes has no previews`() = runBlocking {
        val result = useCase(Result.success(emptyList())).invoke(album("One"))

        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `is empty when the album has no tracks`() = runBlocking {
        val result = useCase(Result.success(listOf(preview("One")))).invoke(album())

        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `passes a repository failure through`() = runBlocking {
        val failure = RuntimeException("offline")

        val result = useCase(Result.failure(failure)).invoke(album("One"))

        assertSame(failure, result.exceptionOrNull())
    }
}
