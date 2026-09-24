package com.ruidoespontaneo.cassette.settings.presentation

import com.ruidoespontaneo.cassette.auth.domain.AuthRepository
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import com.ruidoespontaneo.cassette.auth.domain.usecase.ObserveAuthStateUseCase
import com.ruidoespontaneo.cassette.auth.domain.usecase.SignOutUseCase
import com.ruidoespontaneo.cassette.settings.data.AppLanguage
import com.ruidoespontaneo.cassette.settings.data.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val languageManager = FakeAppLanguageManager()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        ObserveAuthStateUseCase(repository),
        SignOutUseCase(repository),
        languageManager
    )

    @Test
    fun `mirrors whether someone is signed in`() {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isSignedIn)

        repository.user.value = AuthUser(uid = "u1", email = "person@example.com")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isSignedIn)
    }

    @Test
    fun `starts from the app's current language and whether it can be picked`() {
        languageManager.language = AppLanguage.Spanish
        languageManager.isSupported = false

        val state = viewModel().state.value

        assertEquals(AppLanguage.Spanish, state.language)
        assertFalse(state.isLanguagePickerSupported)
    }

    @Test
    fun `SelectLanguage applies it`() {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SelectLanguage(AppLanguage.English))

        assertEquals(AppLanguage.English, languageManager.language)
        assertEquals(AppLanguage.English, viewModel.state.value.language)
    }

    @Test
    fun `SignOut signs out and says so`() = runBlocking {
        repository.user.value = AuthUser(uid = "u1", email = "person@example.com")
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SignOut)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.signOutCalls)
        assertEquals(SettingsEffect.SignedOut, viewModel.effect.first())
    }

    private class FakeAppLanguageManager : AppLanguageManager {
        override var isSupported = true
        var language = AppLanguage.System
        override fun current() = language
        override fun set(language: AppLanguage) {
            this.language = language
        }
    }

    private class FakeAuthRepository : AuthRepository {
        val user = MutableStateFlow<AuthUser?>(null)
        var signOutCalls = 0
            private set

        override val currentUser = user
        override suspend fun signInWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signUpWithEmail(email: String, password: String) = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success(Unit)
        override suspend fun sendPasswordResetEmail(email: String) = Result.success(Unit)
        override fun signOut() {
            signOutCalls++
            user.value = null
        }
    }
}
