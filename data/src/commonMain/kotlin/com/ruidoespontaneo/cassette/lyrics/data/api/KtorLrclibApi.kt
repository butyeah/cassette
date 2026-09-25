package com.ruidoespontaneo.cassette.lyrics.data.api

import com.ruidoespontaneo.cassette.lyrics.data.model.LrclibTrack
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** [LrclibApi] over a client from [com.ruidoespontaneo.cassette.core.network.lrclibHttpClient]. */
class KtorLrclibApi(private val client: HttpClient) : LrclibApi {

    override suspend fun get(artistName: String, trackName: String, albumName: String, duration: Int): LrclibTrack =
        client.get("get") {
            parameter("artist_name", artistName)
            parameter("track_name", trackName)
            parameter("album_name", albumName)
            parameter("duration", duration)
        }.body()

    override suspend fun search(trackName: String, artistName: String): List<LrclibTrack> =
        client.get("search") {
            parameter("track_name", trackName)
            parameter("artist_name", artistName)
        }.body()
}
