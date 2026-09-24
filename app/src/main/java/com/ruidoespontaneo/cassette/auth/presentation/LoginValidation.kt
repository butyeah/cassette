package com.ruidoespontaneo.cassette.auth.presentation

import androidx.annotation.StringRes
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.auth.domain.model.AuthFailure

/** Firebase's own minimum; anything shorter is rejected server-side anyway. */
const val MIN_PASSWORD_LENGTH = 6

private val plausibleEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Shaped like an email (`something@something.tld`) — the server has the final word. */
fun isPlausibleEmail(email: String): Boolean = plausibleEmail.matches(email.trim())

fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

/** Whether the form is complete enough to send: signing in only needs a password at all. */
val LoginUiState.canSubmit: Boolean
    get() = !isLoading && isPlausibleEmail(email) && when (mode) {
        LoginMode.SignIn -> password.isNotEmpty()
        LoginMode.CreateAccount -> isValidPassword(password) && confirmPassword == password
    }

/** A hint under the email field, if any — only once something's been typed, or a reset asked for it. */
@get:StringRes
val LoginUiState.emailHint: Int?
    get() = when {
        email.isNotBlank() && !isPlausibleEmail(email) -> R.string.login_hint_invalid_email
        needsEmailForReset -> R.string.login_hint_email_for_reset
        else -> null
    }

@get:StringRes
val LoginUiState.passwordHint: Int?
    get() = if (mode == LoginMode.CreateAccount && password.isNotEmpty() && !isValidPassword(password)) {
        R.string.login_hint_password_too_short
    } else {
        null
    }

@get:StringRes
val LoginUiState.confirmPasswordHint: Int?
    get() = if (confirmPassword.isNotEmpty() && confirmPassword != password) R.string.login_hint_passwords_differ else null

/** What to tell someone about [this] failure. */
@get:StringRes
val AuthFailure.messageRes: Int
    get() = when (this) {
        AuthFailure.InvalidCredentials -> R.string.login_error_invalid_credentials
        AuthFailure.EmailInUse -> R.string.login_error_email_in_use
        AuthFailure.WeakPassword -> R.string.login_error_weak_password
        AuthFailure.InvalidEmail -> R.string.login_error_invalid_email
        AuthFailure.UserDisabled -> R.string.login_error_user_disabled
        AuthFailure.TooManyRequests -> R.string.login_error_too_many_requests
        AuthFailure.RequiresRecentLogin -> R.string.login_error_requires_recent_login
        AuthFailure.Network -> R.string.login_error_network
        AuthFailure.Unknown -> R.string.login_error_unknown
    }
