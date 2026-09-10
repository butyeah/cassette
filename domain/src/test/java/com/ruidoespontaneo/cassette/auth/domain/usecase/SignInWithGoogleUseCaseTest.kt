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

class SignInWithGoogleUseCaseTest {

    @Test
    fun `forwards the id token to the repository`() = runBlocking {
        var receivedIdToken: String? = null
        val useCase = SignInWithGoogleUseCase(
            fakeRepository { idToken ->
                receivedIdToken = idToken
                Result.success(Unit)
            }
        )

        useCase("id-token-123")

        assertEquals("id-token-123", receivedIdToken)
    }

    @Test
    fun `returns the repository's failure unchanged`() = runBlocking {
        val error = IllegalStateException("boom")
        val useCase = SignInWithGoogleUseCase(fakeRepository { Result.failure(error) })

        val result = useCase("id-token-123")

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun fakeRepository(
        signInWithGoogleIdToken: suspend (idToken: String) -> Result<Unit>
    ) = object : AuthRepository {
        override val currentUser: Flow<AuthUser?> = emptyFlow()
        override suspend fun signInWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String) =
            signInWithGoogleIdToken(idToken)

        override fun signOut() = Unit
    }
}
