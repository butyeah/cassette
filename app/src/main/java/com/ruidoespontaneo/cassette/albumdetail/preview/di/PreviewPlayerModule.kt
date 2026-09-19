package com.ruidoespontaneo.cassette.albumdetail.preview.di

import com.ruidoespontaneo.cassette.albumdetail.preview.Media3PreviewPlayer
import com.ruidoespontaneo.cassette.albumdetail.preview.PreviewPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreviewPlayerModule {

    @Binds
    @Singleton
    abstract fun bindPreviewPlayer(impl: Media3PreviewPlayer): PreviewPlayer
}
