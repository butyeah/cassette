package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SendPasswordResetEmailUseCaseTest {

    @Test
    fun `forwards the email to the repository`() = runBlocking {
        var receivedEmail: String? = null
        val useCase = SendPasswordResetEmailUseCase(fakeRepository { email ->
            receivedEmail = email
            Result.success(Unit)
        })

        useCase("person@example.com")

        assertEquals("person@example.com", receivedEmail)
    }

    @Test
    fun `returns the repository's failure unchanged`() = runBlocking {
        val error = AuthException(AuthFailure.Network)
        val useCase = SendPasswordResetEmailUseCase(fakeRepository { Result.failure(error) })

        assertSame(error, useCase("person@example.com").exceptionOrNull())
    }

    private fun fakeRepository(sendPasswordResetEmail: suspend (email: String) -> Result<Unit>) =
        object : AuthRepository {
            override val currentUser: Flow<AuthUser?> = emptyFlow()
            override suspend fun signInWithEmail(email: String, password: String) = Result.success(Unit)
            override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
            override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(Unit)
            override suspend fun sendPasswordResetEmail(email: String) = sendPasswordResetEmail(email)
            override suspend fun deleteAccount() = Result.success(Unit)
            override fun signOut() = Unit
        }
}
