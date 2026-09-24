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

class DeleteAccountUseCaseTest {

    @Test
    fun `deletes through the repository`() = runBlocking {
        var calls = 0
        val useCase = DeleteAccountUseCase(fakeRepository { calls++; Result.success(Unit) })

        useCase()

        assertEquals(1, calls)
    }

    @Test
    fun `returns the repository's failure unchanged`() = runBlocking {
        val error = AuthException(AuthFailure.RequiresRecentLogin)
        val useCase = DeleteAccountUseCase(fakeRepository { Result.failure(error) })

        assertSame(error, useCase().exceptionOrNull())
    }

    private fun fakeRepository(deleteAccount: suspend () -> Result<Unit>) = object : AuthRepository {
        override val currentUser: Flow<AuthUser?> = emptyFlow()
        override suspend fun signInWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(Unit)
        override suspend fun sendPasswordResetEmail(email: String) = Result.success(Unit)
        override suspend fun deleteAccount() = deleteAccount()
        override fun signOut() = Unit
    }
}
