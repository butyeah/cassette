package com.ruidoespontaneo.cassette.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spacing scale for padding, gaps between elements, etc. Screens should pull from here
 * instead of hardcoding `.dp` literals, so spacing stays consistent app-wide.
 */
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val extraExtraLarge = 32.dp
}

/**
 * Raw font-size constants for one-off `Text` composables that don't go through a
 * [androidx.compose.material3.MaterialTheme.typography] slot (which already carries its own
 * sizes, line heights, and letter spacing — prefer that for anything styled as a proper
 * heading/body/label).
 */
object TextSize {
    val small = 12.sp
    val medium = 14.sp
    val large = 16.sp
    val extraLarge = 20.sp
    val headline = 28.sp
}
