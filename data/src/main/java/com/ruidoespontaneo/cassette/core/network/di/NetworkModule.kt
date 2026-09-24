package com.ruidoespontaneo.cassette.core.network.di

import com.ruidoespontaneo.cassette.core.network.itunesHttpClient
import com.ruidoespontaneo.cassette.core.network.musicBrainzHttpClient
import com.ruidoespontaneo.cassette.data.BuildConfig
import com.ruidoespontaneo.cassette.itunes.data.api.ItunesApi
import com.ruidoespontaneo.cassette.itunes.data.api.KtorItunesApi
import com.ruidoespontaneo.cassette.musicbrainz.data.api.KtorMusicBrainzApi
import com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.Logger
import javax.inject.Qualifier
import javax.inject.Singleton
import timber.log.Timber

/** Distinguishes the MusicBrainz [HttpClient] from any other API client added later. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MusicBrainz

/** Distinguishes the iTunes [HttpClient] — see [MusicBrainz]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Itunes

/** Full request/response bodies to Logcat in debug builds; nothing in release. */
private fun logger(): Logger? = if (BuildConfig.DEBUG) {
    object : Logger {
        override fun log(message: String) = Timber.tag("HTTP").d(message)
    }
} else {
    null
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // One client (and so one rate limiter) for every MusicBrainz call — see RateLimiter.
    @Provides
    @Singleton
    @MusicBrainz
    fun provideMusicBrainzHttpClient(): HttpClient = musicBrainzHttpClient(
        engine = OkHttp.create(),
        userAgent = "Cassette/${BuildConfig.APP_VERSION_NAME} (${BuildConfig.MUSICBRAINZ_CONTACT})",
        logger = logger()
    )

    @Provides
    @Singleton
    fun provideMusicBrainzApi(@MusicBrainz client: HttpClient): MusicBrainzApi = KtorMusicBrainzApi(client)

    @Provides
    @Singleton
    @Itunes
    fun provideItunesHttpClient(): HttpClient = itunesHttpClient(engine = OkHttp.create(), logger = logger())

    @Provides
    @Singleton
    fun provideItunesApi(@Itunes client: HttpClient): ItunesApi = KtorItunesApi(client)
}
