package com.ruidoespontaneo.cassette.auth.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.ui.theme.IconSize

/** Who's signed in — an avatar with their initial and their email — then their settings, grouped in a card. */
@Composable
fun SignedInContent(
    email: String,
    onNotificationsClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(email)
            Column(modifier = Modifier.padding(start = Spacing.large)) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.signed_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(top = Spacing.extraLarge)) {
            SettingsRow(
                label = stringResource(R.string.notifications_title),
                icon = Icons.Filled.Notifications,
                showChevron = true,
                onClick = onNotificationsClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.large))
            SettingsRow(
                label = stringResource(R.string.sign_out),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                showChevron = false,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun Avatar(email: String, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.size(IconSize.avatar)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = email.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    showChevron: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = if (showChevron) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ) {
        Text(label)
    }
}
