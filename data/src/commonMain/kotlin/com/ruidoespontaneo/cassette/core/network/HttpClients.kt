package com.ruidoespontaneo.cassette.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val MUSICBRAINZ_BASE_URL = "https://musicbrainz.org/ws/2/"

private const val ITUNES_BASE_URL = "https://itunes.apple.com/"

/** Where MusicBrainz can reach us about this client — see [musicBrainzUserAgent]. */
private const val MUSICBRAINZ_CONTACT = "https://github.com/butyeah/cassette"

/** The `ApplicationName/Version ( contact )` User-Agent MusicBrainz asks every client to send. */
fun musicBrainzUserAgent(appVersion: String): String = "Cassette/$appVersion ($MUSICBRAINZ_CONTACT)"

/** Both APIs send fields the DTOs don't model; like Moshi before, those are skipped, not errors. */
private val json = Json { ignoreUnknownKeys = true }

/**
 * The client every [com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi] call goes
 * through. [userAgent] comes from [musicBrainzUserAgent]: MusicBrainz rate-limits or blocks generic
 * ones (see https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting).
 * [logger] logs full request and response bodies; pass `null` in release builds.
 */
fun musicBrainzHttpClient(
    engine: HttpClientEngine,
    userAgent: String,
    logger: Logger?,
    rateLimiter: RateLimiter = RateLimiter()
): HttpClient = HttpClient(engine) {
    // Non-2xx responses throw, as they did with Retrofit, instead of failing later as a parse error.
    expectSuccess = true
    install(UserAgent) { agent = userAgent }
    install(ContentNegotiation) { json(json) }
    defaultRequest {
        url(MUSICBRAINZ_BASE_URL)
        // The web service returns XML unless each call asks for JSON.
        url.parameters.append("fmt", "json")
    }
    install(RateLimit) { limiter = rateLimiter }
    logger?.let { install(Logging) { this.logger = it; level = LogLevel.BODY } }
}

/**
 * The client every [com.ruidoespontaneo.cassette.itunes.data.api.ItunesApi] call goes through.
 * Deliberately none of the MusicBrainz client's setup: `fmt=json` would be a stray parameter, and
 * the MusicBrainz User-Agent and 1-request-per-second limit don't apply.
 */
fun itunesHttpClient(engine: HttpClientEngine, logger: Logger?): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(json)
        // iTunes labels its JSON as `text/javascript`.
        json(json, ContentType.parse("text/javascript"))
    }
    defaultRequest { url(ITUNES_BASE_URL) }
    logger?.let { install(Logging) { this.logger = it; level = LogLevel.BODY } }
}

/** The client [com.ruidoespontaneo.cassette.core.firestore.FirestoreRestDocuments] reads through. */
fun firestoreHttpClient(engine: HttpClientEngine, logger: Logger?): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(json) }
    logger?.let { install(Logging) { this.logger = it; level = LogLevel.BODY } }
}

/**
 * The client [com.ruidoespontaneo.cassette.auth.data.rest.FirebaseAuthRestApi] calls through. Never
 * give it a body-logging [logger] outside development: requests carry passwords and tokens.
 */
fun firebaseAuthHttpClient(engine: HttpClientEngine, logger: Logger?): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(json) }
    logger?.let { install(Logging) { this.logger = it; level = LogLevel.HEADERS } }
}
