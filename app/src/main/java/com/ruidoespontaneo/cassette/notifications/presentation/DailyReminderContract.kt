package com.ruidoespontaneo.cassette.notifications.presentation

import com.ruidoespontaneo.cassette.core.mvi.UiEffect
import com.ruidoespontaneo.cassette.core.mvi.UiIntent
import com.ruidoespontaneo.cassette.core.mvi.UiState
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime

data class DailyReminderUiState(
    val scheduledTime: NotificationTime? = null,
    val isPickerExpanded: Boolean = false
) : UiState

sealed interface DailyReminderIntent : UiIntent {
    data object TogglePicker : DailyReminderIntent
    data class Confirm(val hour: Int, val minute: Int) : DailyReminderIntent
    data object Disable : DailyReminderIntent
}

// No one-off events yet — this is here so DailyReminderViewModel has a concrete UiEffect to declare.
sealed interface DailyReminderEffect : UiEffect
