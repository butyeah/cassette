package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveAuthStateUseCaseTest {

    @Test
    fun `returns the repository's currentUser flow unchanged`() = runBlocking {
        val user = AuthUser(uid = "uid-1", email = "person@example.com")
        val useCase = ObserveAuthStateUseCase(fakeRepository(flowOf(user, null)))

        val emissions = useCase().toList()

        assertEquals(listOf(user, null), emissions)
    }

    private fun fakeRepository(currentUserFlow: Flow<AuthUser?>) = object : AuthRepository {
        override val currentUser: Flow<AuthUser?> = currentUserFlow
        override suspend fun signInWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(Unit)
        override fun signOut() = Unit
    }
}
