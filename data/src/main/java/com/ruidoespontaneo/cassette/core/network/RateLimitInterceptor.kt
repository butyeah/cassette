package com.ruidoespontaneo.cassette.core.network

import java.util.concurrent.atomic.AtomicLong
import okhttp3.Interceptor
import okhttp3.Response

/**
 * MusicBrainz's public web service asks clients to stay at or below one
 * request per second per IP; going over gets you temporarily rate-limited
 * (HTTP 503). This interceptor enforces a minimum gap between requests made
 * through the same [okhttp3.OkHttpClient] instance, so as long as all
 * MusicBrainz calls share one client, the app never has to think about it.
 *
 * See https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
 */
class RateLimitInterceptor(
    private val minIntervalMillis: Long = 1_000L
) : Interceptor {

    private val lastRequestAtMillis = AtomicLong(0L)

    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(this) {
            val elapsed = System.currentTimeMillis() - lastRequestAtMillis.get()
            val waitMillis = minIntervalMillis - elapsed
            if (waitMillis > 0) {
                Thread.sleep(waitMillis)
            }
            lastRequestAtMillis.set(System.currentTimeMillis())
        }
        return chain.proceed(chain.request())
    }
}
