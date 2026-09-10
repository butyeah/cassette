package com.ruidoespontaneo.cassette.auth.domain

import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** The signed-in user, or `null` when signed out. Updates whenever auth state changes. */
    val currentUser: Flow<AuthUser?>

    /**
     * @return [Result.success] once signed in, or [Result.failure] with the underlying
     * exception (wrong password, no such account, ...) — callers decide how to surface that.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /** Creates a new account and signs into it. See [signInWithEmail] for the result shape. */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>

    /** Signs in with a Google ID token obtained via Credential Manager. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    fun signOut()
}
