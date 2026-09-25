package com.ruidoespontaneo.cassette.auth.data.rest

import kotlinx.serialization.Serializable

/**
 * Keeps the signed-in session between launches, as one opaque string. The iOS app implements it
 * with the Keychain, since the session holds a refresh token.
 */
interface AuthSessionStore {
    fun load(): String?
    fun save(session: String)
    fun clear()
}

/** What [RestAuthRepository] saves: who's signed in, and the tokens to act as them. */
@Serializable
internal data class AuthSession(
    val uid: String,
    val email: String?,
    val idToken: String,
    val refreshToken: String,
    /** When [idToken] stops being accepted, in epoch seconds. */
    val idTokenExpiresAt: Long
)
