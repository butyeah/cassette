package com.ruidoespontaneo.cassette.notifications.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import com.ruidoespontaneo.cassette.notifications.domain.model.NotificationTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationScheduleRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationContext private val context: Context
) : NotificationScheduleRepository {

    override val scheduledTime: Flow<NotificationTime?> = dataStore.data.map { prefs ->
        val hour = prefs[HOUR_KEY]
        val minute = prefs[MINUTE_KEY]
        if (hour != null && minute != null) NotificationTime(hour, minute) else null
    }

    override suspend fun scheduleDailyReminder(time: NotificationTime) {
        dataStore.edit { prefs ->
            prefs[HOUR_KEY] = time.hour
            prefs[MINUTE_KEY] = time.minute
        }
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextOccurrence(time), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override suspend fun cancelDailyReminder() {
        dataStore.edit { prefs ->
            prefs.remove(HOUR_KEY)
            prefs.remove(MINUTE_KEY)
        }
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun millisUntilNextOccurrence(time: NotificationTime): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    private companion object {
        val HOUR_KEY = intPreferencesKey("hour")
        val MINUTE_KEY = intPreferencesKey("minute")
        const val UNIQUE_WORK_NAME = "daily_reminder"
    }
}
