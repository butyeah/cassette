package com.ruidoespontaneo.cassette.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.presentation.messageRes
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.settings.data.AppLanguage
import com.ruidoespontaneo.cassette.ui.icons.Language
import com.ruidoespontaneo.cassette.ui.theme.IconSize

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.SignedOut, SettingsEffect.AccountDeleted -> onBack()
            }
        }
    }
    SettingsScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onNotificationsClick = onNotificationsClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLanguageDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isSignOutDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        OutlinedCard(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(Spacing.large)
        ) {
            SettingsRow(
                label = stringResource(R.string.notifications_title),
                icon = Icons.Filled.Notifications,
                showChevron = true,
                onClick = onNotificationsClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.large))
            SettingsRow(
                label = stringResource(R.string.settings_language),
                icon = Icons.Filled.Language,
                supportingText = if (state.isLanguagePickerSupported) {
                    stringResource(state.language.labelRes)
                } else {
                    stringResource(R.string.language_follows_phone)
                },
                // Below Android 13 there's no per-app language to pick; the row just explains.
                onClick = if (state.isLanguagePickerSupported) {
                    { isLanguageDialogOpen = true }
                } else {
                    null
                }
            )
            if (state.isSignedIn) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.large))
                SettingsRow(
                    label = stringResource(R.string.sign_out),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = { isSignOutDialogOpen = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.large))
                SettingsRow(
                    label = stringResource(R.string.delete_account),
                    icon = Icons.Filled.Delete,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = { isDeleteDialogOpen = true }
                )
            }
        }
    }
    if (isLanguageDialogOpen) {
        LanguageDialog(
            selected = state.language,
            onSelect = { language ->
                isLanguageDialogOpen = false
                onIntent(SettingsIntent.SelectLanguage(language))
            },
            onDismiss = { isLanguageDialogOpen = false }
        )
    }
    // Stays open while deleting, so the spinner shows; closes once it fails (the failure dialog
    // takes over) or succeeds (the screen goes back).
    if (isDeleteDialogOpen && state.deleteFailure == null) {
        DeleteAccountDialog(
            isDeleting = state.isDeletingAccount,
            onConfirm = { onIntent(SettingsIntent.DeleteAccount) },
            onDismiss = { if (!state.isDeletingAccount) isDeleteDialogOpen = false }
        )
    }
    state.deleteFailure?.let { failure ->
        DeleteFailedDialog(
            failure = failure,
            onSignOut = {
                onIntent(SettingsIntent.DismissDeleteFailure)
                isDeleteDialogOpen = false
                onIntent(SettingsIntent.SignOut)
            },
            onDismiss = {
                onIntent(SettingsIntent.DismissDeleteFailure)
                isDeleteDialogOpen = false
            }
        )
    }
    if (isSignOutDialogOpen) {
        AlertDialog(
            onDismissRequest = { isSignOutDialogOpen = false },
            title = { Text(stringResource(R.string.sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.sign_out_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    isSignOutDialogOpen = false
                    onIntent(SettingsIntent.SignOut)
                }) { Text(stringResource(R.string.sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { isSignOutDialogOpen = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DeleteAccountDialog(isDeleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.delete_account_confirm_title)) },
        text = { Text(stringResource(R.string.delete_account_confirm_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        strokeWidth = IconSize.previewSpinnerStroke,
                        modifier = Modifier.size(IconSize.previewSpinner)
                    )
                } else {
                    Text(stringResource(R.string.delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Why deleting failed. When the sign-in is too old, the way forward is signing in again, so this
 * offers signing out; otherwise it just explains.
 */
@Composable
private fun DeleteFailedDialog(failure: AuthFailure, onSignOut: () -> Unit, onDismiss: () -> Unit) {
    val needsFreshSignIn = failure == AuthFailure.RequiresRecentLogin
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_account_failed_title)) },
        text = { Text(stringResource(failure.messageRes)) },
        confirmButton = {
            if (needsFreshSignIn) {
                TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
        dismissButton = if (needsFreshSignIn) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        } else {
            null
        }
    )
}

/** A row of the settings card; with no [onClick] it's informational and not clickable. */
@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    showChevron: Boolean = false,
    /** For a destructive row, e.g. the error color; `null` keeps the list item defaults. */
    contentColor: Color? = null
) {
    ListItem(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = supportingText?.let { { Text(it) } },
        trailingContent = if (showChevron) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        },
        colors = if (contentColor != null) {
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = contentColor,
                leadingIconColor = contentColor
            )
        } else {
            ListItemDefaults.colors(containerColor = Color.Transparent)
        }
    ) {
        Text(label)
    }
}

/** Radio options for the app's language; picking one applies it straight away. */
@Composable
private fun LanguageDialog(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == selected,
                                onClick = { onSelect(language) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == selected, onClick = null)
                        Text(
                            text = stringResource(language.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = Spacing.medium)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
