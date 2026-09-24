package com.ruidoespontaneo.cassette.auth.domain.model

/** Why an auth call failed, in terms the app can explain to someone — not the backend's own wording. */
enum class AuthFailure {
    /**
     * Wrong password, or no account with that email. Deliberately one case, so the app never
     * reveals which emails have accounts.
     */
    InvalidCredentials,
    EmailInUse,
    WeakPassword,
    InvalidEmail,
    UserDisabled,
    TooManyRequests,
    Network,
    Unknown
}

/** What every failed [com.ruidoespontaneo.cassette.auth.domain.AuthRepository] call fails with. */
class AuthException(val failure: AuthFailure, cause: Throwable? = null) : Exception(failure.name, cause)
