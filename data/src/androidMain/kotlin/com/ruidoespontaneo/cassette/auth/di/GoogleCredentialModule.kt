package com.ruidoespontaneo.cassette.auth.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.ruidoespontaneo.cassette.data.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Credential Manager instances `LoginViewModel` uses for Google Sign-In — the
 * Google-recommended replacement for the deprecated GoogleSignInClient API.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleCredentialModule {

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideGoogleSignInRequest(@ApplicationContext context: Context): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.google_web_client_id))
            .build()
        return GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    }
}
