package com.ruidoespontaneo.cassette

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