package com.ruidoespontaneo.cassette.sdk

import com.ruidoespontaneo.cassette.core.firestore.FirestoreRestDocuments
import com.ruidoespontaneo.cassette.core.network.firestoreHttpClient
import com.ruidoespontaneo.cassette.core.network.itunesHttpClient
import com.ruidoespontaneo.cassette.core.network.musicBrainzHttpClient
import com.ruidoespontaneo.cassette.core.network.musicBrainzUserAgent
import com.ruidoespontaneo.cassette.dayinhistory.data.DayInHistoryRepositoryImpl
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.dayinhistory.domain.usecase.GetAlbumsByDayUseCase
import com.ruidoespontaneo.cassette.itunes.data.ItunesRepositoryImpl
import com.ruidoespontaneo.cassette.itunes.data.api.KtorItunesApi
import com.ruidoespontaneo.cassette.itunes.domain.usecase.GetTrackPreviewsUseCase
import com.ruidoespontaneo.cassette.musicbrainz.data.AlbumTracksRepositoryImpl
import com.ruidoespontaneo.cassette.musicbrainz.data.MusicBrainzRepositoryImpl
import com.ruidoespontaneo.cassette.musicbrainz.data.api.KtorMusicBrainzApi
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.usecase.GetAlbumDetailUseCase
import io.ktor.client.engine.darwin.Darwin

/** The Firebase project the offline pipeline uploads the day index to (app/google-services.json). */
private const val FIREBASE_PROJECT_ID = "cassette-c8951"

/**
 * The iOS app's single entry point into the shared code — what Hilt's graph is on Android, wired
 * by hand. Create one for the app's lifetime: the MusicBrainz rate limit only holds while every
 * call shares its client.
 *
 * Each function unwraps the use case's [Result] and throws on failure, since Kotlin's `Result`
 * doesn't cross into Swift; `@Throws` makes them `async throws` there instead of crashing.
 */
class CassetteSdk(appVersion: String) {

    private val firestore = FirestoreRestDocuments(firestoreHttpClient(Darwin.create(), logger = null), FIREBASE_PROJECT_ID)

    private val musicBrainzRepository = MusicBrainzRepositoryImpl(
        KtorMusicBrainzApi(musicBrainzHttpClient(Darwin.create(), musicBrainzUserAgent(appVersion), logger = null))
    )

    private val getAlbumsByDay = GetAlbumsByDayUseCase(DayInHistoryRepositoryImpl(firestore))

    private val getAlbumDetail = GetAlbumDetailUseCase(musicBrainzRepository, AlbumTracksRepositoryImpl(firestore))

    private val getTrackPreviews = GetTrackPreviewsUseCase(
        ItunesRepositoryImpl(KtorItunesApi(itunesHttpClient(Darwin.create(), logger = null)))
    )

    /** See [GetAlbumsByDayUseCase]: newest year first. */
    @Throws(Exception::class)
    suspend fun albumsByDay(month: Int, day: Int): List<AlbumsByYear> = getAlbumsByDay(month, day).getOrThrow()

    /** See [GetAlbumDetailUseCase]: from the offline index when it has [id], else live MusicBrainz. */
    @Throws(Exception::class)
    suspend fun albumDetail(id: String): AlbumDetail = getAlbumDetail(id).getOrThrow()

    /** See [GetTrackPreviewsUseCase]: preview URL by track position, only for tracks that have one. */
    @Throws(Exception::class)
    suspend fun trackPreviews(album: AlbumDetail): Map<Int, String> = getTrackPreviews(album).getOrThrow()
}
