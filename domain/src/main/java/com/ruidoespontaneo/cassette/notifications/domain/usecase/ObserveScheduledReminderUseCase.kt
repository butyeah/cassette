package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveScheduledReminderUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository
) {
    operator fun invoke(): Flow<NotificationTime?> = repository.scheduledTime
}
