package com.ruidoespontaneo.cassette.auth.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginRulesTest {

    @Test
    fun `plausible emails need a local part and an at and a dotted domain`() {
        assertTrue(isPlausibleEmail("person@example.com"))
        assertTrue(isPlausibleEmail("  person@example.co.uk "))
        assertFalse(isPlausibleEmail("person"))
        assertFalse(isPlausibleEmail("person@example"))
        assertFalse(isPlausibleEmail("@example.com"))
        assertFalse(isPlausibleEmail("per son@example.com"))
    }

    @Test
    fun `passwords need six characters`() {
        assertFalse(isValidPassword("12345"))
        assertTrue(isValidPassword("123456"))
    }
}
