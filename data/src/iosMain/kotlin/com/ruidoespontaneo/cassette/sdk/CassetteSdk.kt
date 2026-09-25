package com.ruidoespontaneo.cassette.sdk

import com.ruidoespontaneo.cassette.auth.data.rest.AuthSessionStore
import com.ruidoespontaneo.cassette.auth.data.rest.FirebaseAuthRestApi
import com.ruidoespontaneo.cassette.auth.data.rest.RestAuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import com.ruidoespontaneo.cassette.auth.domain.usecase.DeleteAccountUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SendPasswordResetEmailUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignInWithEmailUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignOutUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignUpWithEmailUseCase
import com.ruidoespontaneo.cassette.core.firestore.FirestoreRestDocuments
import com.ruidoespontaneo.cassette.core.network.firebaseAuthHttpClient
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** The Firebase project the offline pipeline uploads the day index to (app/google-services.json). */
private const val FIREBASE_PROJECT_ID = "cassette-c8951"

/**
 * The project's Web API key: `current_key` in app/google-services.json. It identifies the project
 * to Firebase Auth's REST API; it isn't a secret.
 */
private const val FIREBASE_API_KEY = "AIzaSyD-p1IVxbEr-PconNJ6JtCHiJLOAxN7-2w"

/**
 * The iOS app's single entry point into the shared code — what Hilt's graph is on Android, wired
 * by hand. Create one for the app's lifetime: the MusicBrainz rate limit only holds while every
 * call shares its client.
 *
 * Each function unwraps the use case's [Result] and throws on failure, since Kotlin's `Result`
 * doesn't cross into Swift; `@Throws` makes them `async throws` there instead of crashing. Auth
 * calls throw an [com.ruidoespontaneo.cassette.auth.domain.model.AuthException] saying why.
 *
 * [sessionStore] keeps the signed-in session between launches (the app's Keychain).
 */
class CassetteSdk(appVersion: String, sessionStore: AuthSessionStore) {

    private val firestore = FirestoreRestDocuments(firestoreHttpClient(Darwin.create(), logger = null), FIREBASE_PROJECT_ID)

    private val musicBrainzRepository = MusicBrainzRepositoryImpl(
        KtorMusicBrainzApi(musicBrainzHttpClient(Darwin.create(), musicBrainzUserAgent(appVersion), logger = null))
    )

    private val getAlbumsByDay = GetAlbumsByDayUseCase(DayInHistoryRepositoryImpl(firestore))

    private val getAlbumDetail = GetAlbumDetailUseCase(musicBrainzRepository, AlbumTracksRepositoryImpl(firestore))

    private val getTrackPreviews = GetTrackPreviewsUseCase(
        ItunesRepositoryImpl(KtorItunesApi(itunesHttpClient(Darwin.create(), logger = null)))
    )

    private val authRepository = RestAuthRepository(
        FirebaseAuthRestApi(firebaseAuthHttpClient(Darwin.create(), logger = null), FIREBASE_API_KEY),
        sessionStore
    )
    private val observeAuthState = ObserveAuthStateUseCase(authRepository)
    private val signInWithEmail = SignInWithEmailUseCase(authRepository)
    private val signUpWithEmail = SignUpWithEmailUseCase(authRepository)
    private val sendPasswordResetEmail = SendPasswordResetEmailUseCase(authRepository)
    private val deleteAccountUseCase = DeleteAccountUseCase(authRepository)
    private val signOutUseCase = SignOutUseCase(authRepository)

    // Delivers auth changes on the main thread, where SwiftUI state lives.
    private val mainScope = MainScope()

    /** See [GetAlbumsByDayUseCase]: newest year first. */
    @Throws(Exception::class)
    suspend fun albumsByDay(month: Int, day: Int): List<AlbumsByYear> = getAlbumsByDay(month, day).getOrThrow()

    /** See [GetAlbumDetailUseCase]: from the offline index when it has [id], else live MusicBrainz. */
    @Throws(Exception::class)
    suspend fun albumDetail(id: String): AlbumDetail = getAlbumDetail(id).getOrThrow()

    /** See [GetTrackPreviewsUseCase]: preview URL by track position, only for tracks that have one. */
    @Throws(Exception::class)
    suspend fun trackPreviews(album: AlbumDetail): Map<Int, String> = getTrackPreviews(album).getOrThrow()

    /** Who's signed in right now, or `null`. */
    val currentUser: AuthUser? get() = authRepository.currentUser.value

    /**
     * Calls [onChange] on the main thread with who's signed in, now and after every change, until
     * the returned [Subscription] is cancelled. A callback because a Flow doesn't cross into Swift.
     */
    fun observeCurrentUser(onChange: (AuthUser?) -> Unit): Subscription =
        Subscription(observeAuthState().onEach(onChange).launchIn(mainScope))

    /** See [SignInWithEmailUseCase]. */
    @Throws(Exception::class)
    suspend fun signIn(email: String, password: String) = signInWithEmail(email, password).getOrThrow()

    /** Creates the account and signs into it. See [SignUpWithEmailUseCase]. */
    @Throws(Exception::class)
    suspend fun signUp(email: String, password: String) = signUpWithEmail(email, password).getOrThrow()

    /** See [SendPasswordResetEmailUseCase]. */
    @Throws(Exception::class)
    suspend fun sendPasswordReset(email: String) = sendPasswordResetEmail(email).getOrThrow()

    /** Permanently deletes the signed-in account, which also signs it out. See [DeleteAccountUseCase]. */
    @Throws(Exception::class)
    suspend fun deleteAccount() = deleteAccountUseCase().getOrThrow()

    fun signOut() = signOutUseCase()
}

/** Stops a [CassetteSdk.observeCurrentUser] callback. */
class Subscription internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
