package com.ruidoespontaneo.cassette.auth.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthFailureMappingTest {

    @Test
    fun `a wrong password and an unknown account look the same`() {
        assertEquals(
            AuthFailure.InvalidCredentials,
            FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "x").toAuthFailure()
        )
        assertEquals(
            AuthFailure.InvalidCredentials,
            FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "x").toAuthFailure()
        )
    }

    @Test
    fun `specific codes get their own failure`() {
        assertEquals(
            AuthFailure.InvalidEmail,
            FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "x").toAuthFailure()
        )
        assertEquals(
            AuthFailure.UserDisabled,
            FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "x").toAuthFailure()
        )
    }

    @Test
    fun `other Firebase failures map by type`() {
        assertEquals(AuthFailure.WeakPassword, FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "x", "too short").toAuthFailure())
        assertEquals(AuthFailure.EmailInUse, FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "x").toAuthFailure())
        assertEquals(AuthFailure.TooManyRequests, FirebaseTooManyRequestsException("x").toAuthFailure())
        assertEquals(AuthFailure.Network, FirebaseNetworkException("x").toAuthFailure())
    }

    @Test
    fun `a stale sign-in asks for a fresh one`() {
        assertEquals(
            AuthFailure.RequiresRecentLogin,
            FirebaseAuthRecentLoginRequiredException("ERROR_REQUIRES_RECENT_LOGIN", "x").toAuthFailure()
        )
    }

    @Test
    fun `anything else is unknown`() {
        assertEquals(AuthFailure.Unknown, IllegalStateException("boom").toAuthFailure())
    }
}
