package com.ruidoespontaneo.cassette.auth.data.rest

import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.io.IOException

/**
 * Translates a failed REST auth call into an [AuthFailure], matching what Android's
 * `Exception.toAuthFailure()` does for the SDK. An unknown account is reported as
 * [AuthFailure.InvalidCredentials], the same as a wrong password, so which emails have accounts
 * isn't given away.
 */
internal fun Throwable.toRestAuthFailure(): AuthFailure = when (this) {
    is FirebaseAuthRestException -> restAuthFailure(code)
    is HttpRequestTimeoutException, is IOException -> AuthFailure.Network
    else -> AuthFailure.Unknown
}

/** Error codes from https://firebase.google.com/docs/reference/rest/auth. */
internal fun restAuthFailure(code: String): AuthFailure = when (code) {
    "INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "USER_NOT_FOUND" -> AuthFailure.InvalidCredentials
    "EMAIL_EXISTS" -> AuthFailure.EmailInUse
    "WEAK_PASSWORD" -> AuthFailure.WeakPassword
    "INVALID_EMAIL", "MISSING_EMAIL" -> AuthFailure.InvalidEmail
    "USER_DISABLED" -> AuthFailure.UserDisabled
    "TOO_MANY_ATTEMPTS_TRY_LATER" -> AuthFailure.TooManyRequests
    // The sign-in is too old for a sensitive call, or the session has been revoked (the password
    // changed elsewhere): either way, signing in again fixes it.
    "CREDENTIAL_TOO_OLD_LOGIN_AGAIN", "TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN", "INVALID_ID_TOKEN" ->
        AuthFailure.RequiresRecentLogin
    else -> AuthFailure.Unknown
}
