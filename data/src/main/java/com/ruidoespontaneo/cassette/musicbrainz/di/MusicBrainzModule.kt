package com.ruidoespontaneo.cassette.musicbrainz.di

import com.ruidoespontaneo.cassette.musicbrainz.data.AlbumTracksRepositoryImpl
import com.ruidoespontaneo.cassette.musicbrainz.data.MusicBrainzRepositoryImpl
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
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

    // AlbumTracksRepositoryImpl's FirebaseFirestore dependency is provided by
    // DayInHistoryModule, already @InstallIn(SingletonComponent::class) alongside this module.
    @Binds
    @Singleton
    abstract fun bindAlbumTracksRepository(
        impl: AlbumTracksRepositoryImpl
    ): AlbumTracksRepository
}
