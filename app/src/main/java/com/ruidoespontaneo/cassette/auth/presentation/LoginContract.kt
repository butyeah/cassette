package com.ruidoespontaneo.cassette.auth.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** The signed-in user's email, or `null` when signed out. */
    val signedInAs: String? = null,
    val errorMessage: String? = null
) : UiState

sealed interface LoginIntent : UiIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object SignIn : LoginIntent
    data object SignUp : LoginIntent
    data object SignOut : LoginIntent
    data object DismissError : LoginIntent
}

sealed interface LoginEffect : UiEffect {
    /** A sign-in/sign-up/Google sign-in just succeeded — the screen navigates back. */
    data object SignedIn : LoginEffect
}
