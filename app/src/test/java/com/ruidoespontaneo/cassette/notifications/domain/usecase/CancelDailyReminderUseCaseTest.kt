package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelDailyReminderUseCaseTest {

    @Test
    fun `delegates to the repository's cancelDailyReminder`() = runBlocking {
        var cancelled = false
        val repository = object : NotificationScheduleRepository {
            override val scheduledTime: Flow<NotificationTime?> = emptyFlow()
            override suspend fun scheduleDailyReminder(time: NotificationTime) = Unit
            override suspend fun cancelDailyReminder() {
                cancelled = true
            }
        }
        val useCase = CancelDailyReminderUseCase(repository)

        useCase()

        assertTrue(cancelled)
    }
}
