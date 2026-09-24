package com.ruidoespontaneo.cassette.dayinhistory.presentation

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter

/**
 * Formats a month and day the way the current language writes them — "June 17", "17 de junio" —
 * rather than a fixed pattern. Follows the configuration, so it updates when the app's language
 * changes.
 */
@Composable
fun rememberDayFormatter(): DateTimeFormatter {
    // Read from the configuration (not Locale.getDefault()) so a language change recomposes this.
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale)
    }
}
