package com.ruidoespontaneo.cassette.itunes.di

import com.ruidoespontaneo.cassette.itunes.data.ItunesRepositoryImpl
import com.ruidoespontaneo.cassette.itunes.domain.ItunesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ItunesModule {

    @Binds
    @Singleton
    abstract fun bindItunesRepository(
        impl: ItunesRepositoryImpl
    ): ItunesRepository
}
