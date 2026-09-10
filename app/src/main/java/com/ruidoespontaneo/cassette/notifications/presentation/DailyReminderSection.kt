package com.ruidoespontaneo.cassette.notifications.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.R

// Material's conventional alpha for disabled content.
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val ENABLED_CONTENT_ALPHA = 1f
private const val DEFAULT_HOUR = 9
private const val DEFAULT_MINUTE = 0

@Composable
fun DailyReminderSection(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    viewModel: DailyReminderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DailyReminderSectionContent(
        state = state,
        onIntent = viewModel::onIntent,
        enabled = enabled,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyReminderSectionContent(
    state: DailyReminderUiState,
    onIntent: (DailyReminderIntent) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val label = state.scheduledTime?.let {
            stringResource(R.string.daily_reminder_at, it.hour, it.minute)
        } ?: stringResource(R.string.daily_reminder_off)
        Text(
            text = label,
            color = LocalContentColor.current.copy(alpha = if (enabled) ENABLED_CONTENT_ALPHA else DISABLED_CONTENT_ALPHA),
            modifier = Modifier.clickable(enabled = enabled) { onIntent(DailyReminderIntent.TogglePicker) }
        )

        if (state.isPickerExpanded && enabled) {
            val timePickerState = rememberTimePickerState(
                initialHour = state.scheduledTime?.hour ?: DEFAULT_HOUR,
                initialMinute = state.scheduledTime?.minute ?: DEFAULT_MINUTE
            )
            Spacer(Modifier.height(8.dp))
            TimePicker(state = timePickerState)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onIntent(DailyReminderIntent.Confirm(timePickerState.hour, timePickerState.minute))
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
                if (state.scheduledTime != null) {
                    TextButton(onClick = { onIntent(DailyReminderIntent.Disable) }) {
                        Text(stringResource(R.string.turn_off))
                    }
                }
            }
        }
    }
}
