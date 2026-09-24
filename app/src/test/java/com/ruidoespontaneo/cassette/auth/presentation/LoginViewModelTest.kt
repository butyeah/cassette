package com.ruidoespontaneo.cassette.auth.presentation

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.CancellationSignal
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.PrepareGetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SendPasswordResetEmailUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignInWithEmailUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignInWithGoogleUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignOutUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignUpWithEmailUseCase
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `reflects the repository's auth state`() {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)
        idle()
        assertNull(viewModel.state.value.signedInAs)

        repository.emit(AuthUser(uid = "u1", email = "person@example.com"))
        idle()

        assertEquals("person@example.com", viewModel.state.value.signedInAs)
    }

    @Test
    fun `Submit in sign-in mode signs in with the trimmed email`() = runBlocking {
        var received: Pair<String, String>? = null
        val viewModel = viewModel(FakeAuthRepository(signInWithEmailResult = { email, password ->
            received = email to password
            Result.success(Unit)
        }))
        viewModel.fill(email = " person@example.com ", password = "secret")

        viewModel.onIntent(LoginIntent.Submit)
        idle()

        assertEquals("person@example.com" to "secret", received)
        assertEquals(LoginEffect.SignedIn, viewModel.effect.first())
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("", viewModel.state.value.password)
    }

    @Test
    fun `Submit in create-account mode signs up`() {
        var signedUp: String? = null
        val viewModel = viewModel(FakeAuthRepository(signUpWithEmailResult = { email, _ ->
            signedUp = email
            Result.success(Unit)
        }))
        viewModel.onIntent(LoginIntent.ModeChanged(LoginMode.CreateAccount))
        viewModel.fill(email = "new@example.com", password = "secret1", confirmPassword = "secret1")

        viewModel.onIntent(LoginIntent.Submit)
        idle()

        assertEquals("new@example.com", signedUp)
    }

    @Test
    fun `Submit does nothing until the form is complete`() {
        var calls = 0
        val viewModel = viewModel(FakeAuthRepository(
            signInWithEmailResult = { _, _ -> calls++; Result.success(Unit) },
            signUpWithEmailResult = { _, _ -> calls++; Result.success(Unit) }
        ))
        viewModel.fill(email = "not-an-email", password = "secret")
        viewModel.onIntent(LoginIntent.Submit)

        viewModel.onIntent(LoginIntent.ModeChanged(LoginMode.CreateAccount))
        viewModel.fill(email = "new@example.com", password = "secret1", confirmPassword = "secret2")
        viewModel.onIntent(LoginIntent.Submit)
        idle()

        assertEquals(0, calls)
    }

    @Test
    fun `a failure keeps its AuthFailure and stops loading`() {
        val viewModel = viewModel(FakeAuthRepository(signInWithEmailResult = { _, _ ->
            Result.failure(AuthException(AuthFailure.InvalidCredentials))
        }))
        viewModel.fill(email = "person@example.com", password = "wrong")

        viewModel.onIntent(LoginIntent.Submit)
        idle()

        assertEquals(AuthFailure.InvalidCredentials, viewModel.state.value.failure)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `an unexpected exception is reported as Unknown`() {
        val viewModel = viewModel(FakeAuthRepository(signInWithEmailResult = { _, _ ->
            Result.failure(IllegalStateException("boom"))
        }))
        viewModel.fill(email = "person@example.com", password = "secret")

        viewModel.onIntent(LoginIntent.Submit)
        idle()

        assertEquals(AuthFailure.Unknown, viewModel.state.value.failure)
    }

    @Test
    fun `editing a field clears the failure`() {
        val viewModel = viewModel(FakeAuthRepository(signInWithEmailResult = { _, _ ->
            Result.failure(AuthException(AuthFailure.Network))
        }))
        viewModel.fill(email = "person@example.com", password = "secret")
        viewModel.onIntent(LoginIntent.Submit)
        idle()

        viewModel.onIntent(LoginIntent.PasswordChanged("secret!"))

        assertNull(viewModel.state.value.failure)
    }

    @Test
    fun `switching mode clears the passwords and the failure but keeps the email`() {
        val viewModel = viewModel(FakeAuthRepository())
        viewModel.fill(email = "person@example.com", password = "secret")

        viewModel.onIntent(LoginIntent.ModeChanged(LoginMode.CreateAccount))

        val state = viewModel.state.value
        assertEquals(LoginMode.CreateAccount, state.mode)
        assertEquals("person@example.com", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
    }

    @Test
    fun `ForgotPassword sends a reset link to a plausible email`() {
        var resetFor: String? = null
        val viewModel = viewModel(FakeAuthRepository(sendPasswordResetResult = { email ->
            resetFor = email
            Result.success(Unit)
        }))
        viewModel.fill(email = "person@example.com")

        viewModel.onIntent(LoginIntent.ForgotPassword)
        idle()

        assertEquals("person@example.com", resetFor)
        assertEquals("person@example.com", viewModel.state.value.resetEmailSentTo)
    }

    @Test
    fun `ForgotPassword without a usable email asks for one instead`() {
        var calls = 0
        val viewModel = viewModel(FakeAuthRepository(sendPasswordResetResult = { calls++; Result.success(Unit) }))

        viewModel.onIntent(LoginIntent.ForgotPassword)
        idle()

        assertEquals(0, calls)
        assertTrue(viewModel.state.value.needsEmailForReset)

        viewModel.onIntent(LoginIntent.EmailChanged("p"))
        assertFalse(viewModel.state.value.needsEmailForReset)
    }

    @Test
    fun `TogglePasswordVisibility flips it`() {
        val viewModel = viewModel(FakeAuthRepository())

        viewModel.onIntent(LoginIntent.TogglePasswordVisibility)
        assertTrue(viewModel.state.value.isPasswordVisible)

        viewModel.onIntent(LoginIntent.TogglePasswordVisibility)
        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun `signInWithGoogle stops loading without crashing when the credential picker is cancelled`() {
        val viewModel = viewModel(
            repository = FakeAuthRepository(),
            credentialManager = FakeCredentialManager(errorToReport = GetCredentialCancellationException())
        )
        idle()

        viewModel.signInWithGoogle(FakeContext)
        idle()

        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.failure)
    }

    @Test
    fun `SignOut delegates to the repository`() {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(LoginIntent.SignOut)

        assertEquals(1, repository.signOutCalls)
    }

    private fun LoginViewModel.fill(email: String? = null, password: String? = null, confirmPassword: String? = null) {
        email?.let { onIntent(LoginIntent.EmailChanged(it)) }
        password?.let { onIntent(LoginIntent.PasswordChanged(it)) }
        confirmPassword?.let { onIntent(LoginIntent.ConfirmPasswordChanged(it)) }
    }

    private fun viewModel(
        repository: AuthRepository,
        credentialManager: CredentialManager = FakeCredentialManager()
    ) = LoginViewModel(
        observeAuthState = ObserveAuthStateUseCase(repository),
        signInWithEmailUseCase = SignInWithEmailUseCase(repository),
        signUpWithEmailUseCase = SignUpWithEmailUseCase(repository),
        signInWithGoogleUseCase = SignInWithGoogleUseCase(repository),
        sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(repository),
        signOutUseCase = SignOutUseCase(repository),
        credentialManager = credentialManager,
        getCredentialRequest = GetCredentialRequest(credentialOptions = listOf(fakeCredentialOption))
    )

    /** A no-op [Context] — [FakeCredentialManager] never actually touches it. */
    private object FakeContext : ContextWrapper(null)

    /**
     * Just to satisfy [GetCredentialRequest]'s non-empty-list requirement — unlike the library's
     * own option types (e.g. `GetPasswordOption`), [GetCustomCredentialOption] takes its request
     * Bundles as-is instead of building them internally, so it doesn't touch any of Bundle's
     * put/get methods, which aren't mocked in this project's plain-JUnit setup.
     */
    private val fakeCredentialOption = GetCustomCredentialOption(
        type = "cassette.test.credential",
        requestData = Bundle(),
        candidateQueryData = Bundle(),
        isSystemProviderRequired = false
    )

    /**
     * A minimal [CredentialManager] fake — it's an interface, so no mocking library is needed.
     * Only the overload `signInWithGoogle` actually calls is exercised; the rest exist purely to
     * satisfy the interface.
     */
    private class FakeCredentialManager(
        private val errorToReport: GetCredentialException? = null
    ) : CredentialManager {
        override fun getCredentialAsync(
            context: Context,
            request: GetCredentialRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
        ) {
            errorToReport?.let(callback::onError)
        }

        override fun getCredentialAsync(
            context: Context,
            pendingGetCredentialHandle: PrepareGetCredentialResponse.PendingGetCredentialHandle,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
        ): Unit = throw UnsupportedOperationException("not used in these tests")

        override fun prepareGetCredentialAsync(
            request: GetCredentialRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<PrepareGetCredentialResponse, GetCredentialException>
        ): Unit = throw UnsupportedOperationException("not used in these tests")

        override fun createCredentialAsync(
            context: Context,
            request: CreateCredentialRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>
        ): Unit = throw UnsupportedOperationException("not used in these tests")

        override fun clearCredentialStateAsync(
            request: ClearCredentialStateRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<Void?, ClearCredentialException>
        ): Unit = throw UnsupportedOperationException("not used in these tests")

        override fun createSettingsPendingIntent(): PendingIntent =
            throw UnsupportedOperationException("not used in these tests")
    }

    private class FakeAuthRepository(
        private val signInWithEmailResult: suspend (String, String) -> Result<Unit> =
            { _, _ -> Result.success(Unit) },
        private val signUpWithEmailResult: suspend (String, String) -> Result<Unit> =
            { _, _ -> Result.success(Unit) },
        private val signInWithGoogleResult: suspend (String) -> Result<Unit> = { Result.success(Unit) },
        private val sendPasswordResetResult: suspend (String) -> Result<Unit> = { Result.success(Unit) }
    ) : AuthRepository {
        private val userFlow = MutableStateFlow<AuthUser?>(null)
        var signOutCalls = 0
            private set
        var lastGoogleIdToken: String? = null
            private set

        override val currentUser: Flow<AuthUser?> = userFlow

        fun emit(user: AuthUser?) {
            userFlow.value = user
        }

        override suspend fun signInWithEmail(email: String, password: String) =
            signInWithEmailResult(email, password)

        override suspend fun signUpWithEmail(email: String, password: String) =
            signUpWithEmailResult(email, password)

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
            lastGoogleIdToken = idToken
            return signInWithGoogleResult(idToken)
        }

        override suspend fun sendPasswordResetEmail(email: String) = sendPasswordResetResult(email)

        override fun signOut() {
            signOutCalls++
        }
    }
}
