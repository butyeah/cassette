package com.ruidoespontaneo.cassette.notifications.domain.usecase

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveScheduledReminderUseCaseTest {

    @Test
    fun `returns the repository's scheduledTime flow unchanged`() = runBlocking {
        val time = NotificationTime(hour = 9, minute = 30)
        val useCase = ObserveScheduledReminderUseCase(fakeRepository(flowOf(time, null)))

        val emissions = useCase().toList()

        assertEquals(listOf(time, null), emissions)
    }

    private fun fakeRepository(scheduledTimeFlow: Flow<NotificationTime?>) =
        object : NotificationScheduleRepository {
            override val scheduledTime: Flow<NotificationTime?> = scheduledTimeFlow
            override suspend fun scheduleDailyReminder(time: NotificationTime) = Unit
            override suspend fun cancelDailyReminder() = Unit
        }
}
