package com.ruidoespontaneo.cassette.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatchingAuthCall { firebaseAuth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        runCatchingAuthCall { firebaseAuth.createUserWithEmailAndPassword(email, password).await() }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        runCatchingAuthCall {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
        }

    override fun signOut() = firebaseAuth.signOut()

    private suspend fun runCatchingAuthCall(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as an auth failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Firebase auth call failed")
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email)
}
