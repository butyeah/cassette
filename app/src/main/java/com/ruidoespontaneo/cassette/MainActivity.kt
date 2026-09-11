package com.ruidoespontaneo.cassette

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ruidoespontaneo.cassette.dayinhistory.di.DayFormatter
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // DateTimeFormatter (java.time) requires API 26, but compileOptions.isCoreLibraryDesugaringEnabled
    // backports it down to minSdk 24 — lint's NewApi check doesn't recognize desugaring coverage for
    // a field's declared type (only for call sites), so this is a known false positive.
    @SuppressLint("NewApi")
    @Inject
    @DayFormatter
    lateinit var dayFormatter: DateTimeFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CassetteTheme {
                CassetteApp(dayFormatter = dayFormatter)
            }
        }
    }
}