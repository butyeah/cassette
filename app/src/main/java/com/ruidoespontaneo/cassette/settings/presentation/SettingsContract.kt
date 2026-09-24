package com.ruidoespontaneo.cassette.settings.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.settings.data.AppLanguage

data class SettingsUiState(
    /** Sign out only shows while someone is signed in. */
    val isSignedIn: Boolean = false,
    val language: AppLanguage = AppLanguage.System,
    /** Whether the app can pick its own language (Android 13+); otherwise it follows the phone. */
    val isLanguagePickerSupported: Boolean = false
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class SelectLanguage(val language: AppLanguage) : SettingsIntent
    data object SignOut : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    /** Sign-out went through — the screen goes back to Profile, now showing the sign-in form. */
    data object SignedOut : SettingsEffect
}
