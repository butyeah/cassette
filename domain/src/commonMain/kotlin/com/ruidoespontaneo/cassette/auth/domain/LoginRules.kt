package com.ruidoespontaneo.cassette.auth.domain

/** Firebase's own minimum; anything shorter is rejected server-side anyway. */
const val MIN_PASSWORD_LENGTH = 6

private val plausibleEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Shaped like an email (`something@something.tld`) — the server has the final word. */
fun isPlausibleEmail(email: String): Boolean = plausibleEmail.matches(email.trim())

fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH
