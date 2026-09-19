package com.ruidoespontaneo.cassette.itunes.data.api

import com.ruidoespontaneo.cassette.itunes.data.model.ItunesResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * iTunes Search API (https://performance-partners.apple.com/search-api). No auth; roughly 20
 * requests a minute per IP. The base URL is configured once in
 * [com.ruidoespontaneo.cassette.core.network.di.NetworkModule].
 *
 * Responses come back as `text/javascript` rather than JSON, which Retrofit's Moshi converter
 * doesn't care about — it parses the body regardless of content type.
 */
interface ItunesApi {

    /** An album ([collectionId]) and its songs, each with a `previewUrl`. */
    @GET("lookup")
    suspend fun lookupAlbumSongs(
        @Query("id") collectionId: Long,
        // A song's availability (and so its previewUrl) is per storefront; null means iTunes' default (US).
        @Query("country") country: String? = null,
        @Query("entity") entity: String = "song"
    ): ItunesResponse

    /** Albums matching [term]; used when we have no Apple Music link to take an exact id from. */
    @GET("search")
    suspend fun searchAlbums(
        @Query("term") term: String,
        @Query("entity") entity: String = "album",
        @Query("limit") limit: Int = 5
    ): ItunesResponse
}
