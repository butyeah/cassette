package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime

class ScheduleDailyReminderUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository
) {
    suspend operator fun invoke(time: NotificationTime) = repository.scheduleDailyReminder(time)
}
