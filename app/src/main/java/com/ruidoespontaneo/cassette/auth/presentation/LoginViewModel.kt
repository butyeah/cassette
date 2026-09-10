package com.ruidoespontaneo.cassette.auth.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
    private val signOutUseCase: SignOutUseCase,
    private val credentialManager: CredentialManager,
    private val getCredentialRequest: GetCredentialRequest
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
            LoginIntent.SignOut -> signOutUseCase()
            LoginIntent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    /**
     * Signs in with Google via Credential Manager — the Google-recommended replacement for the
     * deprecated GoogleSignInClient API. `getCredential` needs an activity [Context], so the
     * caller (the composable button) passes its own rather than this ViewModel holding one.
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            try {
                val credential = credentialManager.getCredential(context, getCredentialRequest).credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    signInWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                }
            } catch (e: GetCredentialException) {
                // The user dismissed the picker or no credential is available — not an
                // app-level error worth surfacing.
            }
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
