package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumCacheExtras
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

    private val detail = AlbumDetail(
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
    fun `uses the cached tracklist and streaming links when the cache has tracks`() = runBlocking {
        val cachedTracks = listOf(Track(position = 1, title = "New Born", lengthMs = 400_000))
        val cachedLinks = StreamingLinks(spotify = "https://open.spotify.com/album/abc")
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(detail) },
                getAlbumTracks = { error("must not fall back to a live lookup when the cache has tracks") }
            ),
            albumTracksRepository = fakeTracksRepository {
                Result.success(AlbumCacheExtras(tracks = cachedTracks, streamingLinks = cachedLinks))
            }
        )

        val result = useCase("album-1")

        assertEquals(cachedTracks, result.getOrNull()?.tracks)
        assertEquals(cachedLinks, result.getOrNull()?.streamingLinks)
    }

    @Test
    fun `falls back to a live tracklist lookup when the cache has no tracks`() = runBlocking {
        val liveTracks = listOf(Track(position = 1, title = "Bliss", lengthMs = 350_000))
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(detail) },
                getAlbumTracks = { Result.success(liveTracks) }
            ),
            albumTracksRepository = fakeTracksRepository { Result.success(AlbumCacheExtras()) }
        )

        val result = useCase("album-1")

        assertEquals(liveTracks, result.getOrNull()?.tracks)
    }

    @Test
    fun `falls back to a live tracklist lookup when the cache read fails`() = runBlocking {
        val liveTracks = listOf(Track(position = 1, title = "Bliss", lengthMs = 350_000))
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(detail) },
                getAlbumTracks = { Result.success(liveTracks) }
            ),
            albumTracksRepository = fakeTracksRepository { Result.failure(IllegalStateException("offline")) }
        )

        val result = useCase("album-1")

        assertEquals(liveTracks, result.getOrNull()?.tracks)
    }

    @Test
    fun `a failed live fallback still succeeds with an empty tracklist`() = runBlocking {
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.success(detail) },
                getAlbumTracks = { Result.failure(IllegalStateException("rate limited")) }
            ),
            albumTracksRepository = fakeTracksRepository { Result.success(AlbumCacheExtras()) }
        )

        val result = useCase("album-1")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Track>(), result.getOrNull()?.tracks)
    }

    @Test
    fun `returns the metadata lookup's failure unchanged, without touching the tracks cache`() = runBlocking {
        val error = IllegalStateException("boom")
        val useCase = useCase(
            musicBrainzRepository = fakeMusicBrainz(
                getAlbumDetail = { Result.failure(error) },
                getAlbumTracks = { error("must not be called") }
            ),
            albumTracksRepository = fakeTracksRepository { error("must not be called") }
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
        getCachedExtras: suspend (id: String) -> Result<AlbumCacheExtras>
    ) = object : AlbumTracksRepository {
        override suspend fun getCachedExtras(id: String): Result<AlbumCacheExtras> = getCachedExtras(id)
    }
}
