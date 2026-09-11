package com.ruidoespontaneo.cassette.auth.presentation

import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.theme.Spacing

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onSignedIn: () -> Unit,
    onNotificationsClick: () -> Unit,
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
    LoginScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNotificationsClick = onNotificationsClick,
        onGoogleSignInClick = viewModel::signInWithGoogle,
        modifier = modifier
    )
}

@Composable
private fun LoginScreenContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    onNotificationsClick: () -> Unit,
    onGoogleSignInClick: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(Spacing.large)) {
        if (state.signedInAs != null) {
            SignedInContent(
                email = state.signedInAs,
                onNotificationsClick = onNotificationsClick,
                onSignOut = { onIntent(LoginIntent.SignOut) }
            )
        } else {
            SignedOutContent(state = state, onIntent = onIntent, onGoogleSignInClick = onGoogleSignInClick)
        }
    }
}

@Composable
private fun SignedOutContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    onGoogleSignInClick: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.profile_greeting), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.profile_sign_in_invitation), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(Spacing.large))
        OutlinedTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.small))
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
            Spacer(Modifier.height(Spacing.small))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(Spacing.large))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Button(onClick = { onIntent(LoginIntent.SignIn) }, enabled = !state.isLoading) {
                Text(stringResource(R.string.sign_in))
            }
            TextButton(onClick = { onIntent(LoginIntent.SignUp) }, enabled = !state.isLoading) {
                Text(stringResource(R.string.sign_up))
            }
        }
        Spacer(Modifier.height(Spacing.large))
        GoogleSignInButton(onClick = onGoogleSignInClick, enabled = !state.isLoading)
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: (Context) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Button(enabled = enabled, onClick = { onClick(context) }, modifier = modifier) {
        Text(stringResource(R.string.sign_in_with_google))
    }
}
