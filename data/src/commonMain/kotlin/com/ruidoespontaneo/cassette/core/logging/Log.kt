package com.ruidoespontaneo.cassette.core.logging

/** Timber on Android (so it reaches Crashlytics), the console on iOS. */
internal expect fun logError(throwable: Throwable, message: String)
