package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAlbumDetailUseCaseTest {

    private val liveDetail = AlbumDetail(
        id = "album-1",
        title = "Origin of Symmetry",
        artistName = "Muse",
        primaryType = "Album",
        firstReleaseDate = LocalDate.of(2001, 6, 17),
        genres = listOf("Rock"),
        ratingValue = 4.0,
        ratingVotesCount = 10
    )

    @Test
    fun `uses the cached album detail as-is when the cache has it, without any live call`() = runBlocking {
        val cachedDetail = liveDetail.copy(
            ratingValue = null,
            ratingVotesCount = 0,
            tracks = listOf(Track(position = 1, title = "New Born", lengthMs = 400_000)),
            streamingLinks = StreamingLinks(spotify = "https://open.spotify.com/album/abc")
        )
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { error("must not call the live metadata lookup on a cache hit") },
                getAlbumTracks = { error("must not call the live tracklist lookup on a cache hit") }
            ),
            albumTracksRepository = fakeTracksRepository { Result.success(cachedDetail) }
        )

        val result = useCase("album-1")

        assertEquals(cachedDetail, result.getOrNull())
    }

    @Test
    fun `falls back to a full live lookup when the cache has nothing for this album`() = runBlocking {
        val liveTracks = listOf(Track(position = 1, title = "Bliss", lengthMs = 350_000))
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(liveDetail) },
                getAlbumTracks = { Result.success(liveTracks) }
            ),
            albumTracksRepository = fakeTracksRepository { Result.success(null) }
        )

        val result = useCase("album-1")

        assertEquals(liveTracks, result.getOrNull()?.tracks)
        assertEquals(4.0, result.getOrNull()?.ratingValue)
    }

    @Test
    fun `falls back to a full live lookup when the cache read fails`() = runBlocking {
        val liveTracks = listOf(Track(position = 1, title = "Bliss", lengthMs = 350_000))
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(liveDetail) },
                getAlbumTracks = { Result.success(liveTracks) }
            ),
            albumTracksRepository = fakeTracksRepository { Result.failure(IllegalStateException("offline")) }
        )

        val result = useCase("album-1")

        assertEquals(liveTracks, result.getOrNull()?.tracks)
    }

    @Test
    fun `a failed live tracklist fallback still succeeds with an empty tracklist`() = runBlocking {
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(liveDetail) },
                getAlbumTracks = { Result.failure(IllegalStateException("rate limited")) }
            ),
            albumTracksRepository = fakeTracksRepository { Result.success(null) }
        )

        val result = useCase("album-1")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Track>(), result.getOrNull()?.tracks)
    }

    @Test
    fun `returns the live metadata lookup's failure unchanged, on a cache miss`() = runBlocking {
        val error = IllegalStateException("boom")
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.failure(error) },
                getAlbumTracks = { error("must not be called") }
            ),
            albumTracksRepository = fakeTracksRepository { Result.success(null) }
        )

        val result = useCase("album-1")

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun useCase(
        musicBrainzRepository: MusicBrainzRepository,
        albumTracksRepository: AlbumTracksRepository
    ) = GetAlbumDetailUseCase(musicBrainzRepository, albumTracksRepository)

    private fun fakeMusicBrainz(
        getAlbumDetail: suspend (id: String) -> Result<AlbumDetail>,
        getAlbumTracks: suspend (id: String) -> Result<List<Track>>
    ) = object : MusicBrainzRepository {
        override suspend fun getAlbumsByDate(
            from: LocalDate,
            to: LocalDate,
            limit: Int,
            offset: Int
        ): Result<List<Album>> = error("not used by this test")

        override suspend fun getAlbumDetail(id: String): Result<AlbumDetail> = getAlbumDetail(id)

        override suspend fun getAlbumTracks(id: String): Result<List<Track>> = getAlbumTracks(id)
    }

    private fun fakeTracksRepository(
        getCachedAlbumDetail: suspend (id: String) -> Result<AlbumDetail?>
    ) = object : AlbumTracksRepository {
        override suspend fun getCachedAlbumDetail(id: String): Result<AlbumDetail?> = getCachedAlbumDetail(id)
    }
}
