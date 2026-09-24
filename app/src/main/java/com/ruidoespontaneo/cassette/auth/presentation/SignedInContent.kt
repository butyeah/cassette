package com.ruidoespontaneo.cassette.auth.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.ui.theme.IconSize

/** Who's signed in: an avatar with their initial, and their email. Settings live behind the gear. */
@Composable
fun SignedInContent(email: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
