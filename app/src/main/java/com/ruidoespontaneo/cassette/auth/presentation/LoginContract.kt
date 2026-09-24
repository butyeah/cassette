package com.ruidoespontaneo.cassette.auth.presentation

import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState

enum class LoginMode { SignIn, CreateAccount }

data class LoginUiState(
    val mode: LoginMode = LoginMode.SignIn,
    val email: String = "",
    val password: String = "",
    /** Only asked for in [LoginMode.CreateAccount]. */
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    /** The signed-in user's email, or `null` when signed out. */
    val signedInAs: String? = null,
    /** Why the last sign-in, sign-up or reset failed, until the form changes. */
    val failure: AuthFailure? = null,
    /** Where a password reset link was just sent, to confirm it. */
    val resetEmailSentTo: String? = null,
    /** "Forgot password?" was tapped without a usable email to send the link to. */
    val needsEmailForReset: Boolean = false
) : UiState

sealed interface LoginIntent : UiIntent {
    data class ModeChanged(val mode: LoginMode) : LoginIntent
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent

    /** Signs in or creates the account, depending on [LoginUiState.mode]. Ignored until [canSubmit]. */
    data object Submit : LoginIntent
    data object ForgotPassword : LoginIntent
    data object DismissError : LoginIntent
}

sealed interface LoginEffect : UiEffect {
    /** A sign-in/sign-up/Google sign-in just succeeded — the screen navigates back. */
    data object SignedIn : LoginEffect
}
