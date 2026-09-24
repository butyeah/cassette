package com.ruidoespontaneo.cassette.auth.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure

/**
 * Translates a Firebase Auth failure into an [AuthFailure]. An unknown account is reported as
 * [AuthFailure.InvalidCredentials], the same as a wrong password, so which emails have accounts
 * isn't given away.
 */
internal fun Exception.toAuthFailure(): AuthFailure = when (this) {
    // Before the more general checks below: these are subclasses of theirs.
    is FirebaseAuthWeakPasswordException -> AuthFailure.WeakPassword
    is FirebaseAuthRecentLoginRequiredException -> AuthFailure.RequiresRecentLogin
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == ERROR_INVALID_EMAIL) AuthFailure.InvalidEmail else AuthFailure.InvalidCredentials

    is FirebaseAuthInvalidUserException ->
        if (errorCode == ERROR_USER_DISABLED) AuthFailure.UserDisabled else AuthFailure.InvalidCredentials

    is FirebaseAuthUserCollisionException -> AuthFailure.EmailInUse
    is FirebaseTooManyRequestsException -> AuthFailure.TooManyRequests
    is FirebaseNetworkException -> AuthFailure.Network
    else -> AuthFailure.Unknown
}

private const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"
private const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
