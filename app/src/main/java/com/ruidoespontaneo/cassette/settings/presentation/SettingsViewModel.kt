package com.ruidoespontaneo.cassette.settings.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignOutUseCase
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.settings.data.AppLanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val appLanguageManager: AppLanguageManager
) : MviViewModel<SettingsUiState, SettingsIntent, SettingsEffect>(
    SettingsUiState(
        language = appLanguageManager.current(),
        isLanguagePickerSupported = appLanguageManager.isSupported
    )
) {

    init {
        observeAuthState()
            .onEach { user -> setState { copy(isSignedIn = user != null) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectLanguage -> {
                appLanguageManager.set(intent.language)
                setState { copy(language = intent.language) }
            }

            SettingsIntent.SignOut -> {
                signOutUseCase()
                sendEffect { SettingsEffect.SignedOut }
            }
        }
    }
}
