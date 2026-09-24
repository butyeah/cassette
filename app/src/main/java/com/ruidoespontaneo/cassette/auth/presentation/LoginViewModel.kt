package com.ruidoespontaneo.cassette.auth.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ruidoespontaneo.cassette.auth.domain.model.AuthException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SendPasswordResetEmailUseCase
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
import timber.log.Timber

@HiltViewModel
class LoginViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
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
            is LoginIntent.ModeChanged -> setState {
                // A fresh form per mode: a sign-in password shouldn't carry into a new account.
                copy(
                    mode = intent.mode,
                    password = "",
                    confirmPassword = "",
                    failure = null,
                    resetEmailSentTo = null,
                    needsEmailForReset = false
                )
            }

            is LoginIntent.EmailChanged -> setState {
                copy(email = intent.email, failure = null, resetEmailSentTo = null, needsEmailForReset = false)
            }

            is LoginIntent.PasswordChanged -> setState { copy(password = intent.password, failure = null) }
            is LoginIntent.ConfirmPasswordChanged ->
                setState { copy(confirmPassword = intent.confirmPassword, failure = null) }

            LoginIntent.TogglePasswordVisibility -> setState { copy(isPasswordVisible = !isPasswordVisible) }
            LoginIntent.Submit -> submit()
            LoginIntent.ForgotPassword -> sendPasswordReset()
            LoginIntent.SignOut -> signOutUseCase()
            LoginIntent.DismissError -> setState { copy(failure = null) }
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
                // app-level error worth surfacing to the UI, but still worth a breadcrumb.
                Timber.w(e, "Google credential request failed or was cancelled")
            }
        }
    }

    private fun submit() {
        if (!currentState.canSubmit) return
        val email = currentState.email.trim()
        val password = currentState.password
        when (currentState.mode) {
            LoginMode.SignIn -> runAuthAction { signInWithEmailUseCase(email, password) }
            LoginMode.CreateAccount -> runAuthAction { signUpWithEmailUseCase(email, password) }
        }
    }

    private fun sendPasswordReset() {
        val email = currentState.email.trim()
        if (!isPlausibleEmail(email)) {
            setState { copy(needsEmailForReset = true) }
            return
        }
        setState { copy(isLoading = true, failure = null, resetEmailSentTo = null) }
        viewModelScope.launch {
            sendPasswordResetEmailUseCase(email)
                .onSuccess { setState { copy(isLoading = false, resetEmailSentTo = email) } }
                .onFailure { error -> setState { copy(isLoading = false, failure = error.toAuthFailure()) } }
        }
    }

    private fun signInWithGoogle(idToken: String) {
        runAuthAction { signInWithGoogleUseCase(idToken) }
    }

    private fun runAuthAction(action: suspend () -> Result<Unit>) {
        setState { copy(isLoading = true, failure = null, resetEmailSentTo = null) }
        viewModelScope.launch {
            action()
                .onSuccess {
                    setState { copy(isLoading = false, password = "", confirmPassword = "") }
                    sendEffect { LoginEffect.SignedIn }
                }
                .onFailure { error -> setState { copy(isLoading = false, failure = error.toAuthFailure()) } }
        }
    }

    // The repository always fails with an AuthException; anything else is a bug worth a generic message.
    private fun Throwable.toAuthFailure(): AuthFailure = (this as? AuthException)?.failure ?: AuthFailure.Unknown
}
