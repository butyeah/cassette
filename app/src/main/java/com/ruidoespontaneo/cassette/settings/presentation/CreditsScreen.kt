package com.ruidoespontaneo.cassette.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme

/** Where the app's album data comes from: one card per source, with its licence and a link to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.credits_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(text = stringResource(R.string.credits_intro), style = MaterialTheme.typography.bodyMedium)
            credits.forEach { CreditCard(it) }
        }
    }
}

@Composable
private fun CreditCard(credit: Credit) {
    val uriHandler = LocalUriHandler.current
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = Spacing.large, top = Spacing.large, end = Spacing.large, bottom = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Text(text = stringResource(credit.nameRes), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(credit.contributionRes), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(credit.licenseRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { uriHandler.openUri(credit.url) }, contentPadding = PaddingValues(0.dp)) {
                Text(credit.url.removePrefix("https://"))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreditsScreenPreview() {
    CassetteTheme { CreditsScreen(onBack = {}) }
}
