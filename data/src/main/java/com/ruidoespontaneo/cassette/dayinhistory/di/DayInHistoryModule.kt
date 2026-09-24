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
import javax.inject.Singleton

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
    }
}
