package com.ruidoespontaneo.cassette.dayinhistory.domain

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album

interface DayInHistoryRepository {

    /**
     * Albums released on [day] of [month], across every year — MusicBrainz's search API can't
     * answer this (no leading wildcards on `date`, and one query per year is impractically slow),
     * so this reads from a Firestore collection pre-populated offline from MusicBrainz's bulk
     * data dump instead.
     *
     * @return [Result.success] with the matches (possibly empty), or [Result.failure] with the
     * underlying exception on a Firestore error — callers decide how to surface that to the UI.
     */
    suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>>
}
