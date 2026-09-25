package com.ruidoespontaneo.cassette.facts.di

import com.ruidoespontaneo.cassette.facts.data.WikidataAlbumFactsRepository
import com.ruidoespontaneo.cassette.facts.domain.AlbumFactsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FactsModule {

    // A singleton so its cache lasts the whole session.
    @Binds
    @Singleton
    abstract fun bindAlbumFactsRepository(
        impl: WikidataAlbumFactsRepository
    ): AlbumFactsRepository
}
