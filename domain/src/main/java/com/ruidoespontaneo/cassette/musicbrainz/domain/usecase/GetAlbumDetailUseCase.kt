package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumCacheExtras
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import javax.inject.Inject

/**
 * Full detail for one album, looked up by its MusicBrainz id — see
 * [MusicBrainzRepository.getAlbumDetail]. Tracks and streaming links are layered on top of that
 * metadata from [AlbumTracksRepository]'s offline Firestore cache first, falling back to a live
 * [MusicBrainzRepository.getAlbumTracks] call only when the cache has no tracks for this album
 * (streaming links have no such fallback — see [AlbumTracksRepository.getCachedExtras]).
 */
class GetAlbumDetailUseCase @Inject constructor(
    private val musicBrainzRepository: MusicBrainzRepository,
    private val albumTracksRepository: AlbumTracksRepository
) {
    suspend operator fun invoke(id: String): Result<AlbumDetail> {
        val detail = musicBrainzRepository.getAlbumDetail(id).getOrElse { return Result.failure(it) }
        val cached = albumTracksRepository.getCachedExtras(id).getOrDefault(AlbumCacheExtras())
        val tracks = cached.tracks.ifEmpty { musicBrainzRepository.getAlbumTracks(id).getOrDefault(emptyList()) }
        return Result.success(detail.copy(tracks = tracks, streamingLinks = cached.streamingLinks))
    }
}
