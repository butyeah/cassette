package com.ruidoespontaneo.cassette.auth.data.rest

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val IDENTITY_TOOLKIT_URL = "https://identitytoolkit.googleapis.com/v1/"
private const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token"

/**
 * Firebase Authentication's REST API (https://firebase.google.com/docs/reference/rest/auth), for
 * email accounts. What the iOS app signs in through, so it needs neither the Firebase iOS SDK nor
 * an iOS app registered in the Firebase console; the accounts are the same ones Android's SDK uses.
 *
 * [apiKey] is the project's Web API key (`current_key` in app/google-services.json). It identifies
 * the project; it isn't a secret. [client] comes from
 * [com.ruidoespontaneo.cassette.core.network.firebaseAuthHttpClient].
 *
 * Every call throws [FirebaseAuthRestException] when Firebase answers with an error.
 */
class FirebaseAuthRestApi(
    private val client: HttpClient,
    private val apiKey: String
) {

    suspend fun signInWithPassword(email: String, password: String): SignInResponse =
        identityToolkit("accounts:signInWithPassword", EmailPasswordRequest(email, password, returnSecureToken = true))

    suspend fun signUp(email: String, password: String): SignInResponse =
        identityToolkit("accounts:signUp", EmailPasswordRequest(email, password, returnSecureToken = true))

    suspend fun sendPasswordResetEmail(email: String) {
        identityToolkit<PasswordResetRequest, Unit>("accounts:sendOobCode", PasswordResetRequest(requestType = "PASSWORD_RESET", email = email))
    }

    suspend fun deleteAccount(idToken: String) {
        identityToolkit<DeleteAccountRequest, Unit>("accounts:delete", DeleteAccountRequest(idToken))
    }

    /** A new ID token (and possibly a new refresh token) for a signed-in session. */
    suspend fun refreshIdToken(refreshToken: String): RefreshResponse = firebaseCall {
        client.submitForm(
            url = SECURE_TOKEN_URL,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }
        ) {
            parameter("key", apiKey)
        }.body()
    }

    private suspend inline fun <reified Request, reified Response> identityToolkit(
        method: String,
        request: Request
    ): Response = firebaseCall {
        client.post(IDENTITY_TOOLKIT_URL + method) {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /** Turns Firebase's `{"error": {"message": "EMAIL_EXISTS"}}` answers into [FirebaseAuthRestException]. */
    private suspend inline fun <T> firebaseCall(call: () -> T): T = try {
        call()
    } catch (e: ResponseException) {
        throw FirebaseAuthRestException(errorCode(e.response.bodyAsText()), e)
    }

    private fun errorCode(body: String): String {
        val message = runCatching {
            Json.parseToJsonElement(body).jsonObject.getValue("error").jsonObject.getValue("message").jsonPrimitive.content
        }.getOrNull() ?: return ""
        // Some codes carry an explanation: "WEAK_PASSWORD : Password should be at least 6 characters".
        return message.substringBefore(" : ").trim()
    }
}

/** Firebase rejected a call; [code] is its error message, e.g. `EMAIL_EXISTS` (empty if unreadable). */
class FirebaseAuthRestException(val code: String, cause: Throwable? = null) : Exception(code, cause)

// No default values in these requests: kotlinx.serialization leaves out properties that still hold
// their default, and Firebase needs every one of these sent.
@Serializable
private class EmailPasswordRequest(val email: String, val password: String, val returnSecureToken: Boolean)

@Serializable
private class PasswordResetRequest(val requestType: String, val email: String)

@Serializable
private class DeleteAccountRequest(val idToken: String)

@Serializable
class SignInResponse(
    val localId: String,
    val email: String? = null,
    val idToken: String,
    val refreshToken: String,
    /** Seconds, as a string: "3600". */
    val expiresIn: String
)

@Serializable
class RefreshResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    /** Seconds, as a string: "3600". */
    @SerialName("expires_in") val expiresIn: String
)
