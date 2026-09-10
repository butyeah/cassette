package com.ruidoespontaneo.cassette.auth.domain.model

/** The signed-in Firebase user, decoupled from the Firebase SDK's own `FirebaseUser` type. */
data class AuthUser(
    val uid: String,
    val email: String?
)
