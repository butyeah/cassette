package com.ruidoespontaneo.cassette.musicbrainz.data.api

import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseBrowseResponse
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseGroupDetailDto
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments

/** [MusicBrainzApi] over a client from [com.ruidoespontaneo.cassette.core.network.musicBrainzHttpClient]. */
class KtorMusicBrainzApi(private val client: HttpClient) : MusicBrainzApi {

    override suspend fun getAlbumsByDate(query: String, limit: Int, offset: Int): ReleaseSearchResponse =
        client.get("release") {
            parameter("query", query)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    override suspend fun getReleaseGroup(id: String, inc: String): ReleaseGroupDetailDto =
        client.get {
            url { appendPathSegments("release-group", id) }
            parameter("inc", inc)
        }.body()

    override suspend fun getReleasesForReleaseGroup(
        releaseGroupId: String,
        inc: String,
        status: String
    ): ReleaseBrowseResponse =
        client.get("release") {
            parameter("release-group", releaseGroupId)
            parameter("inc", inc)
            parameter("status", status)
        }.body()
}
