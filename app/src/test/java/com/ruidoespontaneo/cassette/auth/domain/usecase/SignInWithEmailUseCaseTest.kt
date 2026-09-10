package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInWithEmailUseCaseTest {

    @Test
    fun `forwards email and password to the repository`() = runBlocking {
        var receivedEmail: String? = null
        var receivedPassword: String? = null
        val useCase = SignInWithEmailUseCase(
            fakeRepository { email, password ->
                receivedEmail = email
                receivedPassword = password
                Result.success(Unit)
            }
        )

        useCase("person@example.com", "hunter2")

        assertEquals("person@example.com", receivedEmail)
        assertEquals("hunter2", receivedPassword)
    }

    @Test
    fun `returns the repository's failure unchanged`() = runBlocking {
        val error = IllegalStateException("boom")
        val useCase = SignInWithEmailUseCase(fakeRepository { _, _ -> Result.failure(error) })

        val result = useCase("person@example.com", "hunter2")

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun fakeRepository(
        signInWithEmail: suspend (email: String, password: String) -> Result<Unit>
    ) = object : AuthRepository {
        override val currentUser: Flow<AuthUser?> = emptyFlow()
        override suspend fun signInWithEmail(email: String, password: String) =
            signInWithEmail(email, password)

        override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(Unit)
        override fun signOut() = Unit
    }
}
