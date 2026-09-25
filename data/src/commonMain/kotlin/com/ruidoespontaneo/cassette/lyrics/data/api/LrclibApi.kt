package com.ruidoespontaneo.cassette.lyrics.data.api

import com.ruidoespontaneo.cassette.lyrics.data.model.LrclibTrack

/**
 * LRCLIB (https://lrclib.net/docs): free, no key, crowd-sourced lyrics. Implemented by
 * [KtorLrclibApi]; the base URL and User-Agent are configured once in
 * [com.ruidoespontaneo.cassette.core.network.lrclibHttpClient].
 */
interface LrclibApi {

    /**
     * The one track matching all four, [duration] within ±2 seconds. Throws a
     * [io.ktor.client.plugins.ClientRequestException] with 404 when there's none.
     */
    suspend fun get(artistName: String, trackName: String, albumName: String, duration: Int): LrclibTrack

    /** Tracks loosely matching [trackName] by [artistName], best first; empty when none do. */
    suspend fun search(trackName: String, artistName: String): List<LrclibTrack>
}
