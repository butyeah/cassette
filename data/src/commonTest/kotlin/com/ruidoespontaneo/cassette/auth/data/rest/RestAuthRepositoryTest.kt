package com.ruidoespontaneo.cassette.auth.data.rest

import com.ruidoespontaneo.cassette.auth.domain.model.AuthException
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure
import com.ruidoespontaneo.cassette.auth.domain.model.AuthUser
import com.ruidoespontaneo.cassette.core.network.firebaseAuthHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RestAuthRepositoryTest {

    private class MemoryStore(var saved: String? = null) : AuthSessionStore {
        override fun load() = saved
        override fun save(session: String) { saved = session }
        override fun clear() { saved = null }
    }

    private val requests = mutableListOf<HttpRequestData>()
    private var clock = 1_000L

    private fun repository(
        store: AuthSessionStore = MemoryStore(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): RestAuthRepository {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        val api = FirebaseAuthRestApi(firebaseAuthHttpClient(engine, logger = null), apiKey = "key")
        return RestAuthRepository(api, store, now = { clock })
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.firebaseError(code: String) =
        json("""{"error": {"code": 400, "message": "$code"}}""", HttpStatusCode.BadRequest)

    private val signedIn = """
        {"localId": "uid-1", "email": "a@b.co", "idToken": "id-1", "refreshToken": "refresh-1", "expiresIn": "3600"}
    """.trimIndent()

    private fun HttpRequestData.jsonBody(): JsonObject =
        Json.parseToJsonElement((body as TextContent).text).jsonObject

    private fun HttpRequestData.formBody(): String = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    @Test
    fun signingInPostsTheCredentialsAndSavesTheSession() = runTest {
        val store = MemoryStore()
        val repository = repository(store) { json(signedIn) }

        repository.signInWithEmail("a@b.co", "secret").getOrThrow()

        val request = requests.single()
        assertEquals("/v1/accounts:signInWithPassword", request.url.encodedPath)
        assertEquals("key", request.url.parameters["key"])
        val body = request.jsonBody()
        assertEquals("a@b.co", body.getValue("email").jsonPrimitive.content)
        assertEquals("secret", body.getValue("password").jsonPrimitive.content)
        assertEquals("true", body.getValue("returnSecureToken").jsonPrimitive.content)
        assertEquals(AuthUser("uid-1", "a@b.co"), repository.currentUser.value)
        assertTrue(store.saved!!.contains("refresh-1"))
    }

    @Test
    fun aSavedSessionIsRestoredAtStart() = runTest {
        val store = MemoryStore()
        repository(store) { json(signedIn) }.signUpWithEmail("a@b.co", "secret").getOrThrow()

        val restored = repository(store) { throw AssertionError("unexpected call") }

        assertEquals(AuthUser("uid-1", "a@b.co"), restored.currentUser.value)
    }

    @Test
    fun anUnreadableSavedSessionCountsAsSignedOut() = runTest {
        val repository = repository(MemoryStore(saved = "not json")) { throw AssertionError("unexpected call") }

        assertNull(repository.currentUser.value)
    }

    @Test
    fun firebaseErrorsMapToAuthFailures() = runTest {
        val cases = mapOf(
            "INVALID_LOGIN_CREDENTIALS" to AuthFailure.InvalidCredentials,
            "EMAIL_EXISTS" to AuthFailure.EmailInUse,
            "WEAK_PASSWORD : Password should be at least 6 characters" to AuthFailure.WeakPassword,
            "INVALID_EMAIL" to AuthFailure.InvalidEmail,
            "USER_DISABLED" to AuthFailure.UserDisabled,
            "TOO_MANY_ATTEMPTS_TRY_LATER" to AuthFailure.TooManyRequests,
            "SOMETHING_NEW" to AuthFailure.Unknown
        )
        for ((code, expected) in cases) {
            val repository = repository { firebaseError(code) }

            val failure = repository.signUpWithEmail("a@b.co", "secret").exceptionOrNull()

            assertEquals(expected, assertIs<AuthException>(failure).failure, code)
            assertNull(repository.currentUser.value)
        }
    }

    @Test
    fun noConnectionIsANetworkFailure() = runTest {
        val repository = repository { throw IOException("offline") }

        val failure = repository.signInWithEmail("a@b.co", "secret").exceptionOrNull()

        assertEquals(AuthFailure.Network, assertIs<AuthException>(failure).failure)
    }

    @Test
    fun passwordResetAsksForAResetEmail() = runTest {
        val repository = repository { json("""{"kind": "identitytoolkit#GetOobConfirmationCodeResponse", "email": "a@b.co"}""") }

        repository.sendPasswordResetEmail("a@b.co").getOrThrow()

        val request = requests.single()
        assertEquals("/v1/accounts:sendOobCode", request.url.encodedPath)
        assertEquals("PASSWORD_RESET", request.jsonBody().getValue("requestType").jsonPrimitive.content)
        assertEquals("a@b.co", request.jsonBody().getValue("email").jsonPrimitive.content)
    }

    @Test
    fun deletingWithAnExpiredTokenRefreshesItFirstThenSignsOut() = runTest {
        val store = MemoryStore()
        val repository = repository(store) { request ->
            when (request.url.encodedPath) {
                "/v1/accounts:signInWithPassword" -> json(signedIn)
                "/v1/token" -> json("""{"id_token": "id-2", "refresh_token": "refresh-2", "expires_in": "3600", "user_id": "uid-1"}""")
                "/v1/accounts:delete" -> json("""{"kind": "identitytoolkit#DeleteAccountResponse"}""")
                else -> throw AssertionError("unexpected ${request.url}")
            }
        }
        repository.signInWithEmail("a@b.co", "secret").getOrThrow()
        clock += 3_600

        repository.deleteAccount().getOrThrow()

        val (refresh, delete) = requests.drop(1)
        assertEquals("securetoken.googleapis.com", refresh.url.host)
        assertTrue(refresh.formBody().contains("grant_type=refresh_token"))
        assertTrue(refresh.formBody().contains("refresh_token=refresh-1"))
        assertEquals("id-2", delete.jsonBody().getValue("idToken").jsonPrimitive.content)
        assertNull(repository.currentUser.value)
        assertNull(store.saved)
    }

    @Test
    fun deletingWithAFreshTokenDoesNotRefresh() = runTest {
        val repository = repository { request ->
            if (request.url.encodedPath == "/v1/accounts:signInWithPassword") json(signedIn) else json("{}")
        }
        repository.signInWithEmail("a@b.co", "secret").getOrThrow()

        repository.deleteAccount().getOrThrow()

        assertEquals(listOf("/v1/accounts:signInWithPassword", "/v1/accounts:delete"), requests.map { it.url.encodedPath })
        assertEquals("id-1", requests.last().jsonBody().getValue("idToken").jsonPrimitive.content)
    }

    @Test
    fun anOldSignInMustSignInAgainBeforeDeleting() = runTest {
        val repository = repository { request ->
            if (request.url.encodedPath == "/v1/accounts:signInWithPassword") json(signedIn) else firebaseError("CREDENTIAL_TOO_OLD_LOGIN_AGAIN")
        }
        repository.signInWithEmail("a@b.co", "secret").getOrThrow()

        val failure = repository.deleteAccount().exceptionOrNull()

        assertEquals(AuthFailure.RequiresRecentLogin, assertIs<AuthException>(failure).failure)
        assertEquals(AuthUser("uid-1", "a@b.co"), repository.currentUser.value)
    }

    @Test
    fun anAccountAlreadyDeletedElsewhereStillSignsOut() = runTest {
        val repository = repository { request ->
            if (request.url.encodedPath == "/v1/accounts:signInWithPassword") json(signedIn) else firebaseError("USER_NOT_FOUND")
        }
        repository.signInWithEmail("a@b.co", "secret").getOrThrow()

        repository.deleteAccount().getOrThrow()

        assertNull(repository.currentUser.value)
    }

    @Test
    fun deletingWhileSignedOutSucceedsWithoutACall() = runTest {
        val repository = repository { throw AssertionError("unexpected call") }

        repository.deleteAccount().getOrThrow()

        assertTrue(requests.isEmpty())
    }

    @Test
    fun signingOutForgetsTheSession() = runTest {
        val store = MemoryStore()
        val repository = repository(store) { json(signedIn) }
        repository.signInWithEmail("a@b.co", "secret").getOrThrow()

        repository.signOut()

        assertNull(repository.currentUser.value)
        assertNull(store.saved)
    }
}
