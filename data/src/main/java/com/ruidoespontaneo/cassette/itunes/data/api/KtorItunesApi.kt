package com.ruidoespontaneo.cassette.itunes.data.api

import com.ruidoespontaneo.cassette.itunes.data.model.ItunesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** [ItunesApi] over a client from [com.ruidoespontaneo.cassette.core.network.itunesHttpClient]. */
class KtorItunesApi(private val client: HttpClient) : ItunesApi {

    override suspend fun lookupAlbumSongs(collectionId: Long, country: String?, entity: String): ItunesResponse =
        client.get("lookup") {
            parameter("id", collectionId)
            // Ktor leaves out null parameters, so no country means iTunes' default storefront.
            parameter("country", country)
            parameter("entity", entity)
        }.body()

    override suspend fun searchAlbums(term: String, entity: String, limit: Int): ItunesResponse =
        client.get("search") {
            parameter("term", term)
            parameter("entity", entity)
            parameter("limit", limit)
        }.body()
}
