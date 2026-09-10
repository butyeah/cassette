package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The signed-in user, or `null` when signed out — see [AuthRepository.currentUser]. */
class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> = repository.currentUser
}
