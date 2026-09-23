package com.ruidoespontaneo.cassette.cover.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ruidoespontaneo.cassette.cover.R

/**
 * Pixelify Sans (SIL OFL, see cover/licenses) — bundled as a variable font, so each weight is the
 * same file with a different `wght` axis value. Variable axes need API 26+; on API 24-25 every
 * weight falls back to the font's default (Regular).
 */
val PixelifySans = FontFamily(
    listOf(
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold
    ).map { weight ->
        Font(
            resId = R.font.pixelify_sans,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
        )
    }
)

private val defaults = Typography()

/** Same as [defaults] for [style], but set in [PixelifySans]. */
private fun pixel(style: TextStyle) = style.copy(fontFamily = PixelifySans)

/**
 * Display, headline, title and label styles use [PixelifySans]; body styles stay on the system font
 * so tracklists and other running text remain easy to read at small sizes.
 */
val Typography = Typography(
    displayLarge = pixel(defaults.displayLarge),
    displayMedium = pixel(defaults.displayMedium),
    displaySmall = pixel(defaults.displaySmall),
    headlineLarge = pixel(defaults.headlineLarge),
    headlineMedium = pixel(defaults.headlineMedium),
    headlineSmall = pixel(defaults.headlineSmall),
    titleLarge = pixel(defaults.titleLarge),
    titleMedium = pixel(defaults.titleMedium),
    titleSmall = pixel(defaults.titleSmall),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = pixel(defaults.labelLarge),
    labelMedium = pixel(defaults.labelMedium),
    labelSmall = pixel(defaults.labelSmall)
)
