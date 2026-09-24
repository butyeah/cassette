package com.ruidoespontaneo.cassette.settings.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The languages the app ships in, plus following the phone. [tag] is the BCP 47 language tag. */
enum class AppLanguage(val tag: String?) {
    System(null),
    English("en"),
    Spanish("es")
}

/** Reads and changes the app's own language, independently of the phone's. */
interface AppLanguageManager {

    /** Whether the app can pick its own language — Android 13+ only. */
    val isSupported: Boolean

    fun current(): AppLanguage

    /** Switches the app to [language]; Android recreates visible activities in it. */
    fun set(language: AppLanguage)
}

/**
 * [AppLanguageManager] on the framework's per-app language API ([LocaleManager], Android 13+). The
 * choice is stored by the system, so it survives restarts. Below Android 13 the app simply follows
 * the phone's language: [isSupported] is false and [set] does nothing.
 */
class FrameworkAppLanguageManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppLanguageManager {

    override val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun current(): AppLanguage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return AppLanguage.System
        val locales = localeManager().applicationLocales
        if (locales.isEmpty) return AppLanguage.System
        val language = locales[0].language
        return AppLanguage.entries.firstOrNull { it.tag == language } ?: AppLanguage.System
    }

    override fun set(language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        localeManager().applicationLocales =
            language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun localeManager(): LocaleManager = context.getSystemService(LocaleManager::class.java)
}
