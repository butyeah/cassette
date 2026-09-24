package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDailyReminderUseCaseTest {

    @Test
    fun `forwards the time to the repository`() = runBlocking {
        var received: NotificationTime? = null
        val useCase = ScheduleDailyReminderUseCase(
            fakeRepository { time -> received = time }
        )

        useCase(NotificationTime(hour = 8, minute = 15))

        assertEquals(NotificationTime(hour = 8, minute = 15), received)
    }

    private fun fakeRepository(
        scheduleDailyReminder: suspend (NotificationTime) -> Unit
    ) = object : NotificationScheduleRepository {
        override val scheduledTime: Flow<NotificationTime?> = emptyFlow()
        override suspend fun scheduleDailyReminder(time: NotificationTime) = scheduleDailyReminder(time)
        override suspend fun cancelDailyReminder() = Unit
    }
}
