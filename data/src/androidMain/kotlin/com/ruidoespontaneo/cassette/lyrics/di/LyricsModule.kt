package com.ruidoespontaneo.cassette.lyrics.di

import com.ruidoespontaneo.cassette.lyrics.data.LrclibLyricsRepository
import com.ruidoespontaneo.cassette.lyrics.domain.LyricsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LyricsModule {

    // A singleton so its cache lasts the whole session.
    @Binds
    @Singleton
    abstract fun bindLyricsRepository(
        impl: LrclibLyricsRepository
    ): LyricsRepository
}
