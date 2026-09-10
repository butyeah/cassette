package com.ruidoespontaneo.cassette.notifications.presentation

import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import com.ruidoespontaneo.cassette.notifications.domain.usecase.CancelDailyReminderUseCase
import com.ruidoespontaneo.cassette.notifications.domain.usecase.ObserveScheduledReminderUseCase
import com.ruidoespontaneo.cassette.notifications.domain.usecase.ScheduleDailyReminderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReminderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reflects the repository's scheduled time`() {
        val repository = FakeNotificationScheduleRepository()
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.state.value.scheduledTime)

        repository.emit(NotificationTime(hour = 9, minute = 0))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NotificationTime(hour = 9, minute = 0), viewModel.state.value.scheduledTime)
    }

    @Test
    fun `TogglePicker flips isPickerExpanded`() {
        val viewModel = viewModel(FakeNotificationScheduleRepository())
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isPickerExpanded)

        viewModel.onIntent(DailyReminderIntent.TogglePicker)
        assertTrue(viewModel.state.value.isPickerExpanded)

        viewModel.onIntent(DailyReminderIntent.TogglePicker)
        assertFalse(viewModel.state.value.isPickerExpanded)
    }

    @Test
    fun `Confirm schedules the reminder and collapses the picker`() {
        var received: NotificationTime? = null
        val repository = FakeNotificationScheduleRepository(onSchedule = { received = it })
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(DailyReminderIntent.TogglePicker)

        viewModel.onIntent(DailyReminderIntent.Confirm(hour = 8, minute = 45))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NotificationTime(hour = 8, minute = 45), received)
        assertFalse(viewModel.state.value.isPickerExpanded)
    }

    @Test
    fun `Disable cancels the reminder and collapses the picker`() {
        var cancelCalls = 0
        val repository = FakeNotificationScheduleRepository(onCancel = { cancelCalls++ })
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(DailyReminderIntent.TogglePicker)

        viewModel.onIntent(DailyReminderIntent.Disable)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, cancelCalls)
        assertFalse(viewModel.state.value.isPickerExpanded)
    }

    private fun viewModel(repository: NotificationScheduleRepository) = DailyReminderViewModel(
        observeScheduledReminder = ObserveScheduledReminderUseCase(repository),
        scheduleDailyReminderUseCase = ScheduleDailyReminderUseCase(repository),
        cancelDailyReminderUseCase = CancelDailyReminderUseCase(repository)
    )

    private class FakeNotificationScheduleRepository(
        private val onSchedule: (NotificationTime) -> Unit = {},
        private val onCancel: () -> Unit = {}
    ) : NotificationScheduleRepository {
        private val timeFlow = MutableStateFlow<NotificationTime?>(null)

        override val scheduledTime: Flow<NotificationTime?> = timeFlow

        fun emit(time: NotificationTime?) {
            timeFlow.value = time
        }

        override suspend fun scheduleDailyReminder(time: NotificationTime) = onSchedule(time)

        override suspend fun cancelDailyReminder() = onCancel()
    }
}
