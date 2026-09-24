package com.ruidoespontaneo.cassette.itunes.domain

import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

interface ItunesRepository {

    /**
     * Preview clips for [album]'s iTunes edition, in iTunes' own disc/track order.
     *
     * Finds the edition from [AlbumDetail.streamingLinks]' Apple Music link when there is one (an
     * exact id), otherwise by searching iTunes for the album's artist and title.
     *
     * @return [Result.success] with an empty list when iTunes has no confident match for [album]
     * (not an error — plenty of albums simply aren't on iTunes). [Result.failure] only on a
     * genuine network/parsing failure.
     */
    suspend fun getPreviews(album: AlbumDetail): Result<List<TrackPreview>>
}
