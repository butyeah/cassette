package com.ruidoespontaneo.cassette.auth.domain.usecase

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import javax.inject.Inject

/** Signs in with a Google ID token obtained via Credential Manager (see `LoginScreen`). */
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<Unit> =
        repository.signInWithGoogleIdToken(idToken)
}
