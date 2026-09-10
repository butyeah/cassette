package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Test

class SignOutUseCaseTest {

    @Test
    fun `delegates to the repository's signOut`() {
        var signedOut = false
        val repository = object : AuthRepository {
            override val currentUser: Flow<AuthUser?> = emptyFlow()
            override suspend fun signInWithEmail(email: String, password: String) = Result.success(Unit)
            override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
            override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(Unit)
            override fun signOut() {
                signedOut = true
            }
        }
        val useCase = SignOutUseCase(repository)

        useCase()

        assertTrue(signedOut)
    }
}
