package com.ruidoespontaneo.cassette.core.network.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ruidoespontaneo.cassette.core.network.cassetteUserAgent
import com.ruidoespontaneo.cassette.core.network.itunesHttpClient
import com.ruidoespontaneo.cassette.core.network.lrclibHttpClient
import com.ruidoespontaneo.cassette.core.network.musicBrainzHttpClient
import com.ruidoespontaneo.cassette.core.network.wikidataHttpClient
import com.ruidoespontaneo.cassette.facts.data.api.KtorWikidataApi
import com.ruidoespontaneo.cassette.facts.data.api.WikidataApi
import com.ruidoespontaneo.cassette.itunes.data.api.ItunesApi
import com.ruidoespontaneo.cassette.itunes.data.api.KtorItunesApi
import com.ruidoespontaneo.cassette.lyrics.data.api.KtorLrclibApi
import com.ruidoespontaneo.cassette.lyrics.data.api.LrclibApi
import com.ruidoespontaneo.cassette.musicbrainz.data.api.KtorMusicBrainzApi
import com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

/** Distinguishes the LRCLIB [HttpClient] — see [MusicBrainz]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Lrclib

/** Distinguishes the Wikidata [HttpClient] — see [MusicBrainz]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Wikidata

/**
 * Full request/response bodies to Logcat in debuggable builds; nothing in release. Read from the
 * app, since a multiplatform library module has no BuildConfig of its own.
 */
private fun logger(context: Context): Logger? = if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
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
    fun provideMusicBrainzHttpClient(@ApplicationContext context: Context): HttpClient = musicBrainzHttpClient(
        engine = OkHttp.create(),
        // The app's own versionName, so it can't drift from app/build.gradle.kts.
        userAgent = cassetteUserAgent(
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        ),
        logger = logger(context)
    )

    @Provides
    @Singleton
    fun provideMusicBrainzApi(@MusicBrainz client: HttpClient): MusicBrainzApi = KtorMusicBrainzApi(client)

    @Provides
    @Singleton
    @Itunes
    fun provideItunesHttpClient(@ApplicationContext context: Context): HttpClient =
        itunesHttpClient(engine = OkHttp.create(), logger = logger(context))

    @Provides
    @Singleton
    fun provideItunesApi(@Itunes client: HttpClient): ItunesApi = KtorItunesApi(client)

    @Provides
    @Singleton
    @Lrclib
    fun provideLrclibHttpClient(@ApplicationContext context: Context): HttpClient = lrclibHttpClient(
        engine = OkHttp.create(),
        userAgent = cassetteUserAgent(
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        ),
        logger = logger(context)
    )

    @Provides
    @Singleton
    fun provideLrclibApi(@Lrclib client: HttpClient): LrclibApi = KtorLrclibApi(client)

    @Provides
    @Singleton
    @Wikidata
    fun provideWikidataHttpClient(@ApplicationContext context: Context): HttpClient = wikidataHttpClient(
        engine = OkHttp.create(),
        userAgent = cassetteUserAgent(
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        ),
        logger = logger(context)
    )

    @Provides
    @Singleton
    fun provideWikidataApi(@Wikidata client: HttpClient): WikidataApi = KtorWikidataApi(client)
}
