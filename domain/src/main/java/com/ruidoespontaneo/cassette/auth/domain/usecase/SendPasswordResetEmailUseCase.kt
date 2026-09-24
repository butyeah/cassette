package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import javax.inject.Inject

class SendPasswordResetEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.sendPasswordResetEmail(email)
}
