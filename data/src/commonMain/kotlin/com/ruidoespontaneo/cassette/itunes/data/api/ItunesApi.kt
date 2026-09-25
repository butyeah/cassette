package com.ruidoespontaneo.cassette.itunes.data.api

import com.ruidoespontaneo.cassette.itunes.data.model.ItunesResponse

/**
 * iTunes Search API (https://performance-partners.apple.com/search-api). No auth; roughly 20
 * requests a minute per IP. Implemented by [KtorItunesApi]; the base URL and the
 * `text/javascript` content type iTunes labels its JSON with are configured once in
 * [com.ruidoespontaneo.cassette.core.network.itunesHttpClient].
 */
interface ItunesApi {

    /** An album ([collectionId]) and its songs, each with a `previewUrl`. */
    suspend fun lookupAlbumSongs(
        collectionId: Long,
        // A song's availability (and so its previewUrl) is per storefront; null means iTunes' default (US).
        country: String? = null,
        entity: String = "song"
    ): ItunesResponse

    /** Albums matching [term]; used when we have no Apple Music link to take an exact id from. */
    suspend fun searchAlbums(
        term: String,
        entity: String = "album",
        limit: Int = 5
    ): ItunesResponse
}
