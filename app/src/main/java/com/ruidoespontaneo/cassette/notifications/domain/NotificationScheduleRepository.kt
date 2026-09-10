package com.ruidoespontaneo.cassette.notifications.domain

import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import kotlinx.coroutines.flow.Flow

interface NotificationScheduleRepository {

    /** The currently scheduled reminder time, or `null` when no reminder is set. */
    val scheduledTime: Flow<NotificationTime?>

    /** Persists [time] and (re)schedules the daily reminder to fire at it, starting today or tomorrow. */
    suspend fun scheduleDailyReminder(time: NotificationTime)

    /** Clears any scheduled reminder. */
    suspend fun cancelDailyReminder()
}
