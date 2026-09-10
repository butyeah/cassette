package com.ruidoespontaneo.cassette.dayinhistory.di

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.ruidoespontaneo.cassette.dayinhistory.data.DayInHistoryRepositoryImpl
import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Qualifier
import javax.inject.Singleton

/** Distinguishes the "one day like today" header's [DateTimeFormatter] from any other added later. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DayFormatter

@Module
@InstallIn(SingletonComponent::class)
abstract class DayInHistoryModule {

    @Binds
    @Singleton
    abstract fun bindDayInHistoryRepository(
        impl: DayInHistoryRepositoryImpl
    ): DayInHistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

        @Provides
        @Singleton
        @DayFormatter
        fun provideDayFormatter(): DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    }
}
