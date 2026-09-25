package com.ruidoespontaneo.cassette.core.logging

import timber.log.Timber

internal actual fun logError(throwable: Throwable, message: String) = Timber.e(throwable, message)
