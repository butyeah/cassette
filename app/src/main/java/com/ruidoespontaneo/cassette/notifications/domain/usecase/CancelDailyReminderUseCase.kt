package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import javax.inject.Inject

class CancelDailyReminderUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository
) {
    suspend operator fun invoke() = repository.cancelDailyReminder()
}
