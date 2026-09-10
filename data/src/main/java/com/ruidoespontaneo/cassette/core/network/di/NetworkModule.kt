package com.ruidoespontaneo.cassette.core.network.di

import com.ruidoespontaneo.cassette.core.network.JsonFormatInterceptor
import com.ruidoespontaneo.cassette.core.network.RateLimitInterceptor
import com.ruidoespontaneo.cassette.core.network.UserAgentInterceptor
import com.ruidoespontaneo.cassette.data.BuildConfig
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

/** Distinguishes the MusicBrainz [Retrofit]/[OkHttpClient] from any other API client added later. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MusicBrainz

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
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(userAgent))
            .addInterceptor(JsonFormatInterceptor())
            // Rate limiting must run last so it only delays actual network
            // hits, not requests served from an eventual cache.
            .addInterceptor(RateLimitInterceptor())
            .addInterceptor(logging)
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
}
