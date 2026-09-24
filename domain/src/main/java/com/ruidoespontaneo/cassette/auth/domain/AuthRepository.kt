package com.ruidoespontaneo.cassette.auth.domain

import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** The signed-in user, or `null` when signed out. Updates whenever auth state changes. */
    val currentUser: Flow<AuthUser?>

    /**
     * @return [Result.success] once signed in, or [Result.failure] with an
     * [com.ruidoespontaneo.cassette.auth.domain.model.AuthException] saying why (wrong
     * credentials, network, ...) — callers decide how to surface that. Every call below fails the
     * same way.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /** Creates a new account and signs into it. See [signInWithEmail] for the result shape. */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>

    /** Signs in with a Google ID token obtained via Credential Manager. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    /** Emails [email] a link to reset its password. */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Permanently deletes the signed-in account, which also signs it out. Fails with
     * [com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure.RequiresRecentLogin] when the
     * sign-in is too old for the backend to allow it. Succeeds when nobody is signed in.
     */
    suspend fun deleteAccount(): Result<Unit>

    fun signOut()
}
