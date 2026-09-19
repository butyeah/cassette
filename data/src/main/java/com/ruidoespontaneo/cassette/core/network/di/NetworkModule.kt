package com.ruidoespontaneo.cassette.core.network.di

import com.ruidoespontaneo.cassette.core.network.JsonFormatInterceptor
import com.ruidoespontaneo.cassette.core.network.RateLimitInterceptor
import com.ruidoespontaneo.cassette.core.network.UserAgentInterceptor
import com.ruidoespontaneo.cassette.data.BuildConfig
import com.ruidoespontaneo.cassette.itunes.data.api.ItunesApi
import com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val MUSICBRAINZ_BASE_URL = "https://musicbrainz.org/ws/2/"

private const val ITUNES_BASE_URL = "https://itunes.apple.com/"

/** Distinguishes the MusicBrainz [Retrofit]/[OkHttpClient] from any other API client added later. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MusicBrainz

/** Distinguishes the iTunes [Retrofit]/[OkHttpClient] — see [MusicBrainz]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Itunes

private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    @MusicBrainz
    fun provideMusicBrainzOkHttpClient(): OkHttpClient {
        val userAgent = "Cassette/${BuildConfig.APP_VERSION_NAME} (${BuildConfig.MUSICBRAINZ_CONTACT})"
        return OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(userAgent))
            .addInterceptor(JsonFormatInterceptor())
            // Rate limiting must run last so it only delays actual network
            // hits, not requests served from an eventual cache.
            .addInterceptor(RateLimitInterceptor())
            .addInterceptor(loggingInterceptor())
            .build()
    }

    @Provides
    @Singleton
    @MusicBrainz
    fun provideMusicBrainzRetrofit(
        @MusicBrainz okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(MUSICBRAINZ_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideMusicBrainzApi(@MusicBrainz retrofit: Retrofit): MusicBrainzApi =
        retrofit.create(MusicBrainzApi::class.java)

    // Deliberately none of the MusicBrainz client's interceptors: JsonFormatInterceptor would
    // append a stray `fmt=json`, and the MusicBrainz User-Agent/1-req-per-second limit don't apply.
    @Provides
    @Singleton
    @Itunes
    fun provideItunesOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor())
        .build()

    @Provides
    @Singleton
    @Itunes
    fun provideItunesRetrofit(
        @Itunes okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ITUNES_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideItunesApi(@Itunes retrofit: Retrofit): ItunesApi =
        retrofit.create(ItunesApi::class.java)
}
