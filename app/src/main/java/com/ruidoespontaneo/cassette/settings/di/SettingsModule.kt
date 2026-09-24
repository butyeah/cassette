package com.ruidoespontaneo.cassette.settings.di

import com.ruidoespontaneo.cassette.settings.data.AppLanguageManager
import com.ruidoespontaneo.cassette.settings.data.FrameworkAppLanguageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindAppLanguageManager(impl: FrameworkAppLanguageManager): AppLanguageManager
}
