package com.ruidoespontaneo.cassette.itunes.domain

private val BRACKETED = Regex("""\([^)]*\)|\[[^\]]*]""")
private val DASH_SUFFIX = Regex("""\s+-\s+.*$""")

/**
 * Letters and numbers in any script: what `\p{L}` and `\p{N}` match. Not a regex, because
 * Kotlin/Native's regex engine doesn't support those classes: compiling `[^\p{L}\p{N}]+` throws
 * PatternSyntaxException, and on iOS takes the whole file, and the app, down with it.
 */
private fun Char.isLetterOrNumber(): Boolean = isLetter() || category in NUMBER_CATEGORIES

private val NUMBER_CATEGORIES = setOf(
    CharCategory.DECIMAL_DIGIT_NUMBER,
    CharCategory.LETTER_NUMBER,
    CharCategory.OTHER_NUMBER
)

private fun String.lettersAndNumbers(): String = filter { it.isLetterOrNumber() }

/**
 * A comparison key for matching the same album or song across MusicBrainz and iTunes, which
 * disagree on decorations: "Song (feat. X)", "Album (Bonus Track Version)", "Song - Remastered
 * 2009". Lowercases, drops bracketed and " - suffix" parts, then strips everything but letters and
 * digits.
 *
 * Falls back to the plain lowercased text when stripping would leave nothing (a title that is
 * entirely a parenthetical, e.g. "(Untitled)"), so such titles still compare equal to themselves.
 */
fun String.normalizedForMatching(): String {
    val lowered = lowercase()
    val stripped = lowered
        .replace(BRACKETED, " ")
        .replace(DASH_SUFFIX, "")
        .lettersAndNumbers()
    return stripped.ifEmpty { lowered.lettersAndNumbers() }
}
