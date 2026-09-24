package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository

class CancelDailyReminderUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository
) {
    suspend operator fun invoke() = repository.cancelDailyReminder()
}
