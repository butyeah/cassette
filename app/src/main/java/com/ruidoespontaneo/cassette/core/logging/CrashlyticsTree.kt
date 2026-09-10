package com.ruidoespontaneo.cassette.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Forwards Timber logs to Firebase Crashlytics: INFO/WARN become log breadcrumbs attached to
 * whatever report Crashlytics ends up filing, and ERROR (with a [Throwable]) is recorded as a
 * non-fatal exception — so [Timber.e] is what actually makes an error show up in the Firebase
 * console. VERBOSE/DEBUG are skipped as logcat-only noise.
 */
class CrashlyticsTree(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.INFO) return
        crashlytics.log(tag?.let { "$it: $message" } ?: message)
        if (t != null && priority >= Log.ERROR) crashlytics.recordException(t)
    }
}
