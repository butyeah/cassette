package com.ruidoespontaneo.cassette.musicbrainz.domain

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

interface AlbumTracksRepository {

    /**
     * Full cached detail for album [id] (a release-group MBID) — everything
     * [MusicBrainzRepository.getAlbumDetail] would return except its rating (this offline
     * Firestore index has no live rating data, and rating changes too often to freeze into a
     * dump anyway, so [AlbumDetail.ratingValue] is always `null` here).
     *
     * @return [Result.success] with `null` when [id] isn't cached yet (not "album doesn't
     * exist") — callers should fall back to a live [MusicBrainzRepository.getAlbumDetail] +
     * [MusicBrainzRepository.getAlbumTracks] lookup in that case. [Result.failure] only on a
     * genuine Firestore read failure, which callers should treat the same as "not cached" rather
     * than surfacing it as an error, since this is a cache, not the source of truth.
     */
    suspend fun getCachedAlbumDetail(id: String): Result<AlbumDetail?>
}
