package com.ruidoespontaneo.cassette.auth.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignInWithEmailUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignInWithGoogleUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignOutUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignUpWithEmailUseCase
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase
) : MviViewModel<LoginUiState, LoginIntent, LoginEffect>(LoginUiState()) {

    init {
        observeAuthState()
            .onEach { user -> setState { copy(signedInAs = user?.email) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> setState { copy(email = intent.email, errorMessage = null) }
            is LoginIntent.PasswordChanged ->
                setState { copy(password = intent.password, errorMessage = null) }

            LoginIntent.SignIn -> signIn()
            LoginIntent.SignUp -> signUp()
            is LoginIntent.GoogleSignInResult -> signInWithGoogle(intent.idToken)
            LoginIntent.SignOut -> signOutUseCase()
            LoginIntent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    private fun signIn() {
        val (email, password) = currentState.email to currentState.password
        runAuthAction { signInWithEmailUseCase(email, password) }
    }

    private fun signUp() {
        val (email, password) = currentState.email to currentState.password
        runAuthAction { signUpWithEmailUseCase(email, password) }
    }

    private fun signInWithGoogle(idToken: String) {
        runAuthAction { signInWithGoogleUseCase(idToken) }
    }

    private fun runAuthAction(action: suspend () -> Result<Unit>) {
        setState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            action()
                .onSuccess {
                    setState { copy(isLoading = false, password = "") }
                    sendEffect { LoginEffect.SignedIn }
                }
                .onFailure { error ->
                    setState {
                        copy(isLoading = false, errorMessage = error.message ?: "Couldn't sign in")
                    }
                }
        }
    }
}
