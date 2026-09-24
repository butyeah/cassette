package com.ruidoespontaneo.cassette.auth.presentation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.ui.icons.Visibility
import com.ruidoespontaneo.cassette.ui.icons.VisibilityOff
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import com.ruidoespontaneo.cassette.ui.theme.IconSize
import com.ruidoespontaneo.cassette.ui.theme.ToolbarSize

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = Spacing.large,
                top = Spacing.extraLarge,
                end = Spacing.large,
                bottom = Spacing.large + ToolbarSize.clearance
            )
    ) {
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

/**
 * Google first — one tap, no password — then the email form, which switches between signing in
 * and creating an account rather than offering both at once.
 */
@Composable
private fun SignedOutContent(
    state: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
    onGoogleSignInClick: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.small)
        )
        FilledTonalButton(
            onClick = { onGoogleSignInClick(context) },
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.extraLarge)
        ) {
            Text(stringResource(R.string.continue_with_google))
        }
        OrDivider(Modifier.padding(vertical = Spacing.large))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            EmailForm(state = state, onIntent = onIntent, modifier = Modifier.padding(Spacing.large))
        }
    }
}

@Composable
private fun OrDivider(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.login_or),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.medium)
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun EmailForm(state: LoginUiState, onIntent: (LoginIntent) -> Unit, modifier: Modifier = Modifier) {
    val isCreating = state.mode == LoginMode.CreateAccount
    val submit = { onIntent(LoginIntent.Submit) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        ModeSelector(mode = state.mode, enabled = !state.isLoading, onModeChange = { onIntent(LoginIntent.ModeChanged(it)) })
        OutlinedTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            isError = state.emailHint != null,
            supportingText = state.emailHint?.let { hint -> { Text(stringResource(hint)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress + ContentType.Username }
        )
        PasswordField(
            value = state.password,
            onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            label = stringResource(R.string.password),
            isVisible = state.isPasswordVisible,
            onToggleVisibility = { onIntent(LoginIntent.TogglePasswordVisibility) },
            hint = state.passwordHint,
            contentType = if (isCreating) ContentType.NewPassword else ContentType.Password,
            imeAction = if (isCreating) ImeAction.Next else ImeAction.Done,
            onDone = submit
        )
        AnimatedVisibility(visible = isCreating) {
            PasswordField(
                value = state.confirmPassword,
                onValueChange = { onIntent(LoginIntent.ConfirmPasswordChanged(it)) },
                label = stringResource(R.string.confirm_password),
                isVisible = state.isPasswordVisible,
                onToggleVisibility = { onIntent(LoginIntent.TogglePasswordVisibility) },
                hint = state.confirmPasswordHint,
                contentType = ContentType.NewPassword,
                imeAction = ImeAction.Done,
                onDone = submit
            )
        }
        state.failure?.let { failure ->
            Text(
                text = stringResource(failure.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        state.resetEmailSentTo?.let { email ->
            Text(
                text = stringResource(R.string.reset_email_sent, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Button(onClick = submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    strokeWidth = IconSize.previewSpinnerStroke,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(IconSize.previewSpinner)
                )
            } else {
                Text(stringResource(if (isCreating) R.string.create_account else R.string.sign_in))
            }
        }
        if (!isCreating) {
            TextButton(
                onClick = { onIntent(LoginIntent.ForgotPassword) },
                enabled = !state.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.forgot_password))
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: LoginMode, enabled: Boolean, onModeChange: (LoginMode) -> Unit) {
    val modes = LoginMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == mode,
                onClick = { onModeChange(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
            ) {
                Text(
                    stringResource(
                        when (option) {
                            LoginMode.SignIn -> R.string.sign_in
                            LoginMode.CreateAccount -> R.string.create_account
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    hint: Int?,
    contentType: ContentType,
    imeAction: ImeAction,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = hint != null,
        supportingText = hint?.let { { Text(stringResource(it)) } },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (isVisible) R.string.hide_password else R.string.show_password)
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentType = contentType }
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenSignInPreview() {
    CassetteTheme {
        LoginScreenContent(LoginUiState(email = "person@example.com"), {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenCreateAccountPreview() {
    CassetteTheme {
        LoginScreenContent(
            LoginUiState(mode = LoginMode.CreateAccount, email = "person@", password = "abc", confirmPassword = "abd"),
            {},
            {},
            {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenSignedInPreview() {
    CassetteTheme {
        LoginScreenContent(LoginUiState(signedInAs = "person@example.com"), {}, {}, {})
    }
}
