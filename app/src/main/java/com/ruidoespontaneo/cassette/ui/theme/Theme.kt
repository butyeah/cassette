package com.ruidoespontaneo.cassette.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.ruidoespontaneo.cassette.cover.theme.CoverTheme

/** Cassette's app theme — a thin wrapper around Cover's [CoverTheme], the design system this app builds on. */
@Composable
fun CassetteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = CoverTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
