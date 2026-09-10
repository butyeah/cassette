package com.ruidoespontaneo.cassette.auth.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.notifications.presentation.DailyReminderSection

@Composable
fun SignedInContent(email: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.signed_in_as, email), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        val context = LocalContext.current
        var isNotificationsGranted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }
        NotificationsSetupButton(
            isGranted = isNotificationsGranted,
            onGrantedChange = { isNotificationsGranted = it }
        )
        // The daily reminder is meaningless without notification permission, so it only
        // appears once that's granted.
        if (isNotificationsGranted) {
            Spacer(Modifier.height(16.dp))
            DailyReminderSection()
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
    }
}

/** Requests the Android 13+ notification permission — there's nothing else to "set up" yet. */
@Composable
private fun NotificationsSetupButton(
    isGranted: Boolean,
    onGrantedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onGrantedChange(granted) }

    if (isGranted) {
        Text(stringResource(R.string.notifications_enabled), modifier = modifier)
    } else {
        Button(
            onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = modifier
        ) {
            Text(stringResource(R.string.set_up_notifications))
        }
    }
}

// Below API 33, notifications are enabled by default and there's no permission to request.
private fun isNotificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
