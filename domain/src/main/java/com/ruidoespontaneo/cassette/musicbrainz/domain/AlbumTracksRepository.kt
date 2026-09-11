package com.ruidoespontaneo.cassette.musicbrainz.domain

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track

/** [AlbumTracksRepository.getCachedExtras]'s payload — grouped together because both live in the
 *  same offline-populated Firestore document, so one read covers both. */
data class AlbumCacheExtras(
    val tracks: List<Track> = emptyList(),
    val streamingLinks: StreamingLinks = StreamingLinks()
)

interface AlbumTracksRepository {

    /**
     * Cached tracklist + streaming-service links for album [id] (a release-group MBID), from the
     * offline Firestore index built by `scripts/build_day_index.py`/`upload_day_index.py`.
     *
     * Empty [AlbumCacheExtras.tracks] means "not cached", not necessarily "no tracks" — callers
     * should fall back to a live MusicBrainz tracklist lookup (see
     * [MusicBrainzRepository.getAlbumTracks]). [AlbumCacheExtras.streamingLinks] has no such
     * fallback — MusicBrainz's own bulk dump is the only source for it here — so a missing link
     * there just means "not available for this release", not "not yet fetched".
     *
     * @return [Result.success] (possibly with empty extras) unless the Firestore read itself
     * failed, in which case [Result.failure] — callers should treat that the same as "not cached"
     * rather than surfacing it as an error, since this is a cache, not the source of truth.
     */
    suspend fun getCachedExtras(id: String): Result<AlbumCacheExtras>
}
