package com.ruidoespontaneo.cassette

import android.app.Application
import com.ruidoespontaneo.cassette.core.logging.CrashlyticsTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. Annotating this with [HiltAndroidApp] triggers Hilt's
 * code generation and creates the app-level dependency container that every
 * other Hilt component (activities, view models, ...) attaches to.
 */
@HiltAndroidApp
class CassetteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Planted in all build types, not just release, so Crashlytics gets reports
        // from local/debug runs too.
        Timber.plant(CrashlyticsTree())
    }
}
