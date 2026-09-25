package com.ruidoespontaneo.cassette.auth.data.rest

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import com.ruidoespontaneo.cassette.core.logging.logError
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Refresh an ID token this long before it expires, so it can't lapse on its way to Firebase. */
private const val EXPIRY_MARGIN_SECONDS = 60L

@OptIn(ExperimentalTime::class)
private fun epochSecondsNow(): Long = Clock.System.now().epochSeconds

/**
 * [AuthRepository] over [FirebaseAuthRestApi], for email accounts: the iOS app's, since Android
 * uses the Firebase SDK. The session survives relaunches through [store].
 *
 * Google sign-in isn't supported: the iOS app doesn't offer it.
 */
class RestAuthRepository(
    private val api: FirebaseAuthRestApi,
    private val store: AuthSessionStore,
    private val now: () -> Long = ::epochSecondsNow
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // A session that can't be read back (from an older format, say) counts as signed out.
    private var session: AuthSession? = store.load()?.let { saved ->
        runCatching { json.decodeFromString<AuthSession>(saved) }.getOrNull()
    }

    private val _currentUser = MutableStateFlow(session?.toAuthUser())

    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatchingAuthCall { startSession(api.signInWithPassword(email, password)) }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        runCatchingAuthCall { startSession(api.signUp(email, password)) }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        Result.failure(AuthException(AuthFailure.Unknown))

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        runCatchingAuthCall { api.sendPasswordResetEmail(email) }

    // Cassette keeps no per-user data of its own, so the Auth user is the whole account.
    // Deleting it also signs it out. Succeeds when nobody is signed in, as on Android.
    override suspend fun deleteAccount(): Result<Unit> = runCatchingAuthCall {
        val current = session ?: return@runCatchingAuthCall
        try {
            api.deleteAccount(freshIdToken(current))
        } catch (e: FirebaseAuthRestException) {
            // Already gone (deleted from another device): the goal is met.
            if (e.code != "USER_NOT_FOUND") throw e
        }
        endSession()
    }

    override fun signOut() = endSession()

    private fun startSession(response: SignInResponse) {
        val started = AuthSession(
            uid = response.localId,
            email = response.email,
            idToken = response.idToken,
            refreshToken = response.refreshToken,
            idTokenExpiresAt = now() + response.expiresIn.toLong()
        )
        save(started)
        _currentUser.value = started.toAuthUser()
    }

    private fun endSession() {
        session = null
        store.clear()
        _currentUser.value = null
    }

    private fun save(updated: AuthSession) {
        session = updated
        store.save(json.encodeToString(AuthSession.serializer(), updated))
    }

    /** [current]'s ID token, refreshed first when it has expired or is about to. */
    private suspend fun freshIdToken(current: AuthSession): String {
        if (now() < current.idTokenExpiresAt - EXPIRY_MARGIN_SECONDS) return current.idToken
        val refreshed = api.refreshIdToken(current.refreshToken)
        val updated = current.copy(
            idToken = refreshed.idToken,
            refreshToken = refreshed.refreshToken,
            idTokenExpiresAt = now() + refreshed.expiresIn.toLong()
        )
        save(updated)
        return updated.idToken
    }

    private suspend fun runCatchingAuthCall(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as an auth failure.
            throw e
        } catch (e: Exception) {
            logError(e, "Firebase auth REST call failed")
            Result.failure(AuthException(e.toRestAuthFailure(), e))
        }
    }

    private fun AuthSession.toAuthUser() = AuthUser(uid = uid, email = email)
}
