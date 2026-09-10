package com.ruidoespontaneo.cassette.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ruidoespontaneo.cassette.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onSignedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.SignedIn -> onSignedIn()
            }
        }
    }
    LoginScreenContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
private fun LoginScreenContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (state.signedInAs != null) {
            SignedInContent(email = state.signedInAs, onSignOut = { onIntent(LoginIntent.SignOut) })
        } else {
            SignedOutContent(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun SignedInContent(email: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.signed_in_as, email), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
    }
}

@Composable
private fun SignedOutContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_greeting), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.profile_sign_in_invitation), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onIntent(LoginIntent.SignIn) }, enabled = !state.isLoading) {
                Text(stringResource(R.string.sign_in))
            }
            TextButton(onClick = { onIntent(LoginIntent.SignUp) }, enabled = !state.isLoading) {
                Text(stringResource(R.string.sign_up))
            }
        }
        Spacer(Modifier.height(16.dp))
        GoogleSignInButton(onIntent = onIntent, enabled = !state.isLoading)
    }
}

/**
 * Signs in with Google via Credential Manager — the Google-recommended replacement for the
 * deprecated GoogleSignInClient API. Needs a [android.content.Context], so this platform/UI glue
 * lives here rather than in a use case.
 */
@Composable
private fun GoogleSignInButton(
    onIntent: (LoginIntent) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Button(
        enabled = enabled,
        onClick = {
            coroutineScope.launch {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.google_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                try {
                    val credential = CredentialManager.create(context)
                        .getCredential(context, request)
                        .credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        onIntent(LoginIntent.GoogleSignInResult(idToken))
                    }
                } catch (e: GetCredentialException) {
                    // The user dismissed the picker or no credential is available — not an
                    // app-level error worth surfacing.
                }
            }
        },
        modifier = modifier
    ) {
        Text(stringResource(R.string.sign_in_with_google))
    }
}
