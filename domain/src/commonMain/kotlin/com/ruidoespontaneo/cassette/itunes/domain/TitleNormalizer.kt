package com.ruidoespontaneo.cassette.itunes.domain

private val BRACKETED = Regex("""\([^)]*\)|\[[^\]]*]""")
private val DASH_SUFFIX = Regex("""\s+-\s+.*$""")
private val NOT_ALPHANUMERIC = Regex("""[^\p{L}\p{N}]+""")

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
        .replace(NOT_ALPHANUMERIC, "")
    return stripped.ifEmpty { lowered.replace(NOT_ALPHANUMERIC, "") }
}
