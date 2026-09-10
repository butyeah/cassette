package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import javax.inject.Inject

class ScheduleDailyReminderUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository
) {
    suspend operator fun invoke(time: NotificationTime) = repository.scheduleDailyReminder(time)
}
