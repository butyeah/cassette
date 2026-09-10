package com.ruidoespontaneo.cassette.auth.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ruidoespontaneo.cassette.R

@Composable
fun SignedInContent(
    email: String,
    onNotificationsClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.signed_in_as, email), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Column {
            SettingsRow(label = stringResource(R.string.notifications_title), onClick = onNotificationsClick)
            SettingsRow(label = stringResource(R.string.sign_out), onClick = onSignOut)
        }
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = modifier.clickable(onClick = onClick)
    )
}
