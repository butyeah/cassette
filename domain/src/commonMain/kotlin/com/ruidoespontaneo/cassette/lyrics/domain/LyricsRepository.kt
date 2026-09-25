package com.ruidoespontaneo.cassette.lyrics.domain

import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics

interface LyricsRepository {

    /**
     * Lyrics for [track] by [artist] from [album]. [durationSeconds] narrows the match when it's
     * known.
     *
     * @return [Result.success] with `null` when there are none to be found (not an error: plenty of
     * tracks simply aren't listed). [Result.failure] only on a genuine network/parsing failure.
     */
    suspend fun getLyrics(artist: String, track: String, album: String, durationSeconds: Int?): Result<Lyrics?>
}
