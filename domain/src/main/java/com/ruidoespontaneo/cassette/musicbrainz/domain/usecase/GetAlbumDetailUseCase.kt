package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import javax.inject.Inject

/**
 * Full detail for one album, looked up by its MusicBrainz id. Cache-first: when
 * [AlbumTracksRepository]'s offline Firestore index already has [id] (the common case — see its
 * doc comment), that single cached document replaces the live [MusicBrainzRepository] lookup
 * entirely (metadata, tracklist and streaming links all come from it — only rating is never
 * cached, so it's always `null` on a cache-served album). Only a genuine cache miss falls back to
 * a live [MusicBrainzRepository.getAlbumDetail] + [MusicBrainzRepository.getAlbumTracks] lookup,
 * same as before this cache existed.
 */
class GetAlbumDetailUseCase @Inject constructor(
    private val musicBrainzRepository: MusicBrainzRepository,
    private val albumTracksRepository: AlbumTracksRepository
) {
    suspend operator fun invoke(id: String): Result<AlbumDetail> {
        val cached = albumTracksRepository.getCachedAlbumDetail(id).getOrNull()
        if (cached != null) {
            return Result.success(cached)
        }

        val detail = musicBrainzRepository.getAlbumDetail(id).getOrElse { return Result.failure(it) }
        val tracks = musicBrainzRepository.getAlbumTracks(id).getOrDefault(emptyList())
        return Result.success(detail.copy(tracks = tracks))
    }
}
