package com.ruidoespontaneo.cassette.notifications.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isNotificationsGranted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }

    // The permission can only be revoked from the system settings screen (see
    // notificationSettingsIntent), so re-check on resume to pick up a change made there.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationsGranted = isNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> isNotificationsGranted = granted }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Card(modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(Spacing.large)) {
            Column(modifier = Modifier.padding(Spacing.large)) {
                NotificationPermissionRow(
                    isGranted = isNotificationsGranted,
                    onRequestPermission = {
                        // Below API 33 there's no POST_NOTIFICATIONS permission to request —
                        // notifications are enabled by default, so isNotificationsGranted is
                        // already true and this branch isn't reachable there.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                DailyReminderSection(enabled = isNotificationsGranted, modifier = Modifier.padding(top = Spacing.large))
            }
        }
    }
}

@Composable
private fun NotificationPermissionRow(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGranted) {
        Column(modifier = modifier) {
            Text(stringResource(R.string.notifications_enabled))
            val context = LocalContext.current
            Text(
                text = stringResource(R.string.manage_in_settings),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { context.startActivity(notificationSettingsIntent(context)) }
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onRequestPermission),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = false, onCheckedChange = { onRequestPermission() })
            Text(stringResource(R.string.notifications_permission_label))
        }
    }
}
