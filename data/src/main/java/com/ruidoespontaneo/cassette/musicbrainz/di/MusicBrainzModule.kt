package com.ruidoespontaneo.cassette.musicbrainz.di

import com.ruidoespontaneo.cassette.musicbrainz.data.MusicBrainzRepositoryImpl
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MusicBrainzModule {

    @Binds
    @Singleton
    abstract fun bindMusicBrainzRepository(
        impl: MusicBrainzRepositoryImpl
    ): MusicBrainzRepository
}
