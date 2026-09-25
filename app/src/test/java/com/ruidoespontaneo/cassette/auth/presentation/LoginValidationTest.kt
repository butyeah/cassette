package com.ruidoespontaneo.cassette.auth.presentation

import com.ruidoespontaneo.cassette.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidationTest {

    @Test
    fun `signing in only needs a plausible email and some password`() {
        assertTrue(LoginUiState(email = "person@example.com", password = "x").canSubmit)
        assertFalse(LoginUiState(email = "person@example.com").canSubmit)
        assertFalse(LoginUiState(email = "person", password = "x").canSubmit)
        assertFalse(LoginUiState(email = "person@example.com", password = "x", isLoading = true).canSubmit)
    }

    @Test
    fun `creating an account needs a valid, confirmed password`() {
        val base = LoginUiState(mode = LoginMode.CreateAccount, email = "person@example.com")
        assertTrue(base.copy(password = "secret1", confirmPassword = "secret1").canSubmit)
        assertFalse(base.copy(password = "short", confirmPassword = "short").canSubmit)
        assertFalse(base.copy(password = "secret1", confirmPassword = "secret2").canSubmit)
    }

    @Test
    fun `hints only show once something invalid has been typed`() {
        assertNull(LoginUiState().emailHint)
        assertEquals(R.string.login_hint_invalid_email, LoginUiState(email = "person").emailHint)
        assertEquals(R.string.login_hint_email_for_reset, LoginUiState(needsEmailForReset = true).emailHint)

        assertNull(LoginUiState(password = "abc").passwordHint) // sign-in doesn't judge length
        assertEquals(
            R.string.login_hint_password_too_short,
            LoginUiState(mode = LoginMode.CreateAccount, password = "abc").passwordHint
        )
        assertEquals(
            R.string.login_hint_passwords_differ,
            LoginUiState(password = "secret1", confirmPassword = "secret2").confirmPasswordHint
        )
    }
}
