package com.ruidoespontaneo.cassette.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * The MusicBrainz web service returns XML by default; JSON has to be
 * requested per-call via a `fmt=json` query parameter. Adding it here once
 * means individual [com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi]
 * endpoints don't need to repeat it.
 */
class JsonFormatInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalUrl = chain.request().url
        val url = originalUrl.newBuilder()
            .addQueryParameter("fmt", "json")
            .build()
        val request = chain.request().newBuilder()
            .url(url)
            .build()
        return chain.proceed(request)
    }
}
