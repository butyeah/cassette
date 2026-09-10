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
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
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

    @Test
    fun `reflects the repository's auth state`() {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.state.value.signedInAs)

        repository.emit(AuthUser(uid = "uid-1", email = "person@example.com"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("person@example.com", viewModel.state.value.signedInAs)
    }

    @Test
    fun `EmailChanged and PasswordChanged update the fields and clear the error`() {
        val viewModel = viewModel(FakeAuthRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(LoginIntent.EmailChanged("person@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("hunter2"))

        val state = viewModel.state.value
        assertEquals("person@example.com", state.email)
        assertEquals("hunter2", state.password)
        assertNull(state.errorMessage)
    }

    @Test
    fun `SignIn success clears the password, stops loading, and sends SignedIn`() = runBlocking {
        val viewModel = viewModel(FakeAuthRepository())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(LoginIntent.EmailChanged("person@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("hunter2"))

        viewModel.onIntent(LoginIntent.SignIn)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("", state.password)
        assertNull(state.errorMessage)
        assertEquals(LoginEffect.SignedIn, viewModel.effect.first())
    }

    @Test
    fun `SignIn failure surfaces an error message and stops loading`() {
        val error = IllegalStateException("boom")
        val repository = FakeAuthRepository(signInWithEmailResult = { _, _ -> Result.failure(error) })
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(LoginIntent.SignIn)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `SignUp forwards the current fields to the use case`() = runBlocking {
        var received: Pair<String, String>? = null
        val repository = FakeAuthRepository(
            signUpWithEmailResult = { email, password ->
                received = email to password
                Result.success(Unit)
            }
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(LoginIntent.EmailChanged("new@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("hunter2"))

        viewModel.onIntent(LoginIntent.SignUp)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("new@example.com" to "hunter2", received)
        assertEquals(LoginEffect.SignedIn, viewModel.effect.first())
    }

    @Test
    fun `signInWithGoogle stops loading without crashing when the credential picker is cancelled`() {
        val viewModel = viewModel(
            repository = FakeAuthRepository(),
            credentialManager = FakeCredentialManager(errorToReport = GetCredentialCancellationException())
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.signInWithGoogle(FakeContext)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `SignOut delegates to the repository`() {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(LoginIntent.SignOut)

        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun `DismissError clears the error message`() {
        val repository = FakeAuthRepository(
            signInWithEmailResult = { _, _ -> Result.failure(IllegalStateException("boom")) }
        )
        val viewModel = viewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(LoginIntent.SignIn)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("boom", viewModel.state.value.errorMessage)

        viewModel.onIntent(LoginIntent.DismissError)

        assertNull(viewModel.state.value.errorMessage)
    }

    private fun viewModel(
        repository: AuthRepository,
        credentialManager: CredentialManager = FakeCredentialManager()
    ) = LoginViewModel(
        observeAuthState = ObserveAuthStateUseCase(repository),
        signInWithEmailUseCase = SignInWithEmailUseCase(repository),
        signUpWithEmailUseCase = SignUpWithEmailUseCase(repository),
        signInWithGoogleUseCase = SignInWithGoogleUseCase(repository),
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
        private val signInWithGoogleResult: suspend (String) -> Result<Unit> = { Result.success(Unit) }
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

        override fun signOut() {
            signOutCalls++
        }
    }
}
