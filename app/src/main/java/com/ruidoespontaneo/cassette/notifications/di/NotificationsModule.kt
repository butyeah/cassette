package com.ruidoespontaneo.cassette.notifications.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ruidoespontaneo.cassette.notifications.data.NotificationScheduleRepositoryImpl
import com.ruidoespontaneo.cassette.notifications.domain.NotificationScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindNotificationScheduleRepository(
        impl: NotificationScheduleRepositoryImpl
    ): NotificationScheduleRepository

    companion object {
        @Provides
        @Singleton
        fun provideNotificationDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create {
                context.preferencesDataStoreFile("notification_schedule")
            }
    }
}
