package com.ruidoespontaneo.cassette.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * MusicBrainz requires every client to identify itself with a descriptive
 * `User-Agent` header — generic ones (browser strings, the OkHttp default,
 * an empty string) get rate-limited more aggressively or blocked outright.
 *
 * The expected form is `ApplicationName/Version ( contact )`.
 * See https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
 */
class UserAgentInterceptor(
    private val userAgent: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }
}
