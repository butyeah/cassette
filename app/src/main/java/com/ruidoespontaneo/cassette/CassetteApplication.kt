package com.ruidoespontaneo.cassette

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotating this with [HiltAndroidApp] triggers Hilt's
 * code generation and creates the app-level dependency container that every
 * other Hilt component (activities, view models, ...) attaches to.
 */
@HiltAndroidApp
class CassetteApplication : Application()
