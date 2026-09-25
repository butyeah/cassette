package com.ruidoespontaneo.cassette.lyrics.domain.usecase

import com.ruidoespontaneo.cassette.lyrics.domain.LyricsRepository
import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetTrackLyricsUseCaseTest {

    private val album = AlbumDetail(
        id = "album-1",
        title = "OK Computer",
        artistName = "Radiohead",
        primaryType = "Album",
        firstReleaseDate = LocalDate(1997, 5, 21),
        genres = emptyList(),
        ratingValue = null,
        ratingVotesCount = 0,
        tracks = listOf(
            Track(position = 1, title = "Airbag", lengthMs = 284_499),
            Track(position = 6, title = "Karma Police", lengthMs = null)
        )
    )

    private data class Call(val artist: String, val track: String, val album: String, val durationSeconds: Int?)

    private val calls = mutableListOf<Call>()

    private val useCase = GetTrackLyricsUseCase(
        object : LyricsRepository {
            override suspend fun getLyrics(artist: String, track: String, album: String, durationSeconds: Int?): Result<Lyrics?> {
                calls += Call(artist, track, album, durationSeconds)
                return Result.success(Lyrics.Plain("words"))
            }
        }
    )

    @Test
    fun `asks for the track's title with the album's artist and title, and its length in whole seconds`() = runBlocking {
        val result = useCase(album, position = 1)

        assertEquals(Lyrics.Plain("words"), result.getOrNull())
        assertEquals(Call("Radiohead", "Airbag", "OK Computer", 284), calls.single())
    }

    @Test
    fun `passes no duration when MusicBrainz has none`() = runBlocking {
        useCase(album, position = 6)

        assertNull(calls.single().durationSeconds)
    }

    @Test
    fun `a position that isn't a track has no lyrics, without asking`() = runBlocking {
        val result = useCase(album, position = 3)

        assertNull(result.getOrThrow())
        assertTrue(calls.isEmpty())
    }
}
