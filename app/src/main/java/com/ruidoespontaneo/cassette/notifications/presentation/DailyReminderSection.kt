package com.ruidoespontaneo.cassette.notifications.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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

@Composable
fun DailyReminderSection(
    modifier: Modifier = Modifier,
    viewModel: DailyReminderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DailyReminderSectionContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyReminderSectionContent(
    state: DailyReminderUiState,
    onIntent: (DailyReminderIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val label = state.scheduledTime?.let {
            stringResource(R.string.daily_reminder_at, "%02d:%02d".format(it.hour, it.minute))
        } ?: stringResource(R.string.daily_reminder_off)
        Text(text = label, modifier = Modifier.clickable { onIntent(DailyReminderIntent.TogglePicker) })

        if (state.isPickerExpanded) {
            val timePickerState = rememberTimePickerState(
                initialHour = state.scheduledTime?.hour ?: 9,
                initialMinute = state.scheduledTime?.minute ?: 0
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
