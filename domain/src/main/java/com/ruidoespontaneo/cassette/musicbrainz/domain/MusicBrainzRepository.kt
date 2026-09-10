package com.ruidoespontaneo.cassette.musicbrainz.domain

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import java.time.LocalDate

interface MusicBrainzRepository {

    /**
     * Albums (MusicBrainz releases) whose release date falls within
     * [from, to] inclusive — a whole month or week fetched in one call, to
     * populate a calendar view.
     *
     * @return [Result.success] with the matches (possibly empty), or
     * [Result.failure] with the underlying exception on a network or HTTP
     * error — callers decide how to surface that to the UI.
     */
    suspend fun getAlbumsByDate(
        from: LocalDate,
        to: LocalDate,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Album>>

    /**
     * Full detail for one album (MusicBrainz release group), looked up by
     * its MBID — e.g. [Album.id], however that album was found.
     *
     * @return [Result.success] with the album's detail, or [Result.failure]
     * with the underlying exception on a network or HTTP error — callers
     * decide how to surface that to the UI.
     */
    suspend fun getAlbumDetail(id: String): Result<AlbumDetail>
}
