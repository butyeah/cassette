package com.ruidoespontaneo.cassette.core.network

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * MusicBrainz's public web service asks clients to stay at or below one
 * request per second per IP; going over gets you temporarily rate-limited
 * (HTTP 503). This enforces a minimum gap between requests made through the
 * same [io.ktor.client.HttpClient], so as long as all MusicBrainz calls share
 * one client, the app never has to think about it.
 *
 * Waiting suspends rather than blocking a thread, and only the wait is
 * serialized: the request itself runs outside the lock.
 *
 * See https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
 */
class RateLimiter(
    private val minInterval: Duration = 1.seconds,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private val mutex = Mutex()
    private var lastRequestAt: TimeMark? = null

    suspend fun awaitTurn() {
        mutex.withLock {
            // A negative delay (the gap has already passed) returns immediately.
            lastRequestAt?.let { delay(minInterval - it.elapsedNow()) }
            lastRequestAt = timeSource.markNow()
        }
    }
}

class RateLimitConfig {
    var limiter: RateLimiter = RateLimiter()
}

/**
 * Hooks [Send], so it runs once per actual network call (including retries and
 * redirects), after the request is fully built.
 */
val RateLimit = createClientPlugin("RateLimit", ::RateLimitConfig) {
    val limiter = pluginConfig.limiter
    on(Send) { request ->
        limiter.awaitTurn()
        proceed(request)
    }
}
