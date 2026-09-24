package com.ruidoespontaneo.cassette.settings.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.auth.domain.model.AuthException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.domain.usecase.DeleteAccountUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignOutUseCase
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.settings.data.AppLanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
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

            SettingsIntent.DeleteAccount -> deleteAccount()
            SettingsIntent.DismissDeleteFailure -> setState { copy(deleteFailure = null) }
        }
    }

    private fun deleteAccount() {
        if (currentState.isDeletingAccount) return
        setState { copy(isDeletingAccount = true, deleteFailure = null) }
        viewModelScope.launch {
            deleteAccountUseCase()
                .onSuccess {
                    setState { copy(isDeletingAccount = false) }
                    sendEffect { SettingsEffect.AccountDeleted }
                }
                .onFailure { error ->
                    val failure = (error as? AuthException)?.failure ?: AuthFailure.Unknown
                    setState { copy(isDeletingAccount = false, deleteFailure = failure) }
                }
        }
    }
}
