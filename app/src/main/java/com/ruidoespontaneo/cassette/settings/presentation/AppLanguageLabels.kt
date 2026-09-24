package com.ruidoespontaneo.cassette.settings.presentation

import androidx.annotation.StringRes
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.settings.data.AppLanguage

/** How [this] is named in the language picker — each language in its own words, as Android does. */
@get:StringRes
val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.System -> R.string.language_system_default
        AppLanguage.English -> R.string.language_english
        AppLanguage.Spanish -> R.string.language_spanish
    }
