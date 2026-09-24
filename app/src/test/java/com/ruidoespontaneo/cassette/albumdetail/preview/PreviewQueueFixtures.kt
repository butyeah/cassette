package com.ruidoespontaneo.cassette.albumdetail.preview

import com.ruidoespontaneo.cassette.itunes.domain.ItunesRepository
import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.itunes.domain.usecase.GetTrackPreviewsUseCase
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.LocalDate

/**
 * A real [PreviewQueue] over [player], looking albums up in [albums] (a missing id fails to load)
 * and their previews through [previews].
 */
fun testPreviewQueue(
    player: FakePreviewPlayer,
    scope: CoroutineScope,
    albums: Map<String, AlbumDetail> = emptyMap(),
    previews: suspend (AlbumDetail) -> Result<List<TrackPreview>> = { Result.success(emptyList()) }
): PreviewQueue {
    val musicBrainz = object : MusicBrainzRepository {
        override suspend fun getAlbumsByDate(from: LocalDate, to: LocalDate, limit: Int, offset: Int): Result<List<Album>> =
            error("not used by PreviewQueue")

        override suspend fun getAlbumDetail(id: String): Result<AlbumDetail> =
            albums[id]?.let { Result.success(it) } ?: Result.failure(IllegalStateException("no album $id"))

        override suspend fun getAlbumTracks(id: String): Result<List<Track>> =
            albums[id]?.let { Result.success(it.tracks) } ?: Result.failure(IllegalStateException("no album $id"))
    }
    val noCache = object : AlbumTracksRepository {
        override suspend fun getCachedAlbumDetail(id: String): Result<AlbumDetail?> = Result.success(null)
    }
    val itunes = object : ItunesRepository {
        override suspend fun getPreviews(album: AlbumDetail): Result<List<TrackPreview>> = previews(album)
    }
    return PreviewQueue(player, GetAlbumDetailUseCase(musicBrainz, noCache), GetTrackPreviewsUseCase(itunes), scope)
}
