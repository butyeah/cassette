package com.ruidoespontaneo.cassette.notifications.presentation

import androidx.lifecycle.viewModelScope
import com.ruidoespontaneo.cassette.core.mvi.MviViewModel
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import com.ruidoespontaneo.cassette.notifications.domain.usecase.CancelDailyReminderUseCase
import com.ruidoespontaneo.cassette.notifications.domain.usecase.ObserveScheduledReminderUseCase
import com.ruidoespontaneo.cassette.notifications.domain.usecase.ScheduleDailyReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class DailyReminderViewModel @Inject constructor(
    observeScheduledReminder: ObserveScheduledReminderUseCase,
    private val scheduleDailyReminderUseCase: ScheduleDailyReminderUseCase,
    private val cancelDailyReminderUseCase: CancelDailyReminderUseCase
) : MviViewModel<DailyReminderUiState, DailyReminderIntent, DailyReminderEffect>(DailyReminderUiState()) {

    init {
        observeScheduledReminder()
            .onEach { time -> setState { copy(scheduledTime = time) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DailyReminderIntent) {
        when (intent) {
            DailyReminderIntent.TogglePicker -> setState { copy(isPickerExpanded = !isPickerExpanded) }
            is DailyReminderIntent.Confirm -> {
                viewModelScope.launch {
                    scheduleDailyReminderUseCase(NotificationTime(intent.hour, intent.minute))
                    setState { copy(isPickerExpanded = false) }
                }
            }

            DailyReminderIntent.Disable -> viewModelScope.launch {
                cancelDailyReminderUseCase()
                setState { copy(isPickerExpanded = false) }
            }
        }
    }
}
