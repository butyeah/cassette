package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

private const val MIN_WAVE_CONTRAST = 3f

// WCAG AA for body text — stricter than the waveform's, since this has to be read, not just seen.
private const val MIN_TEXT_CONTRAST = 4.5f

/** WCAG contrast ratio between two opaque colors, from 1 (identical) to 21 (black on white). */
internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * [color], pushed toward black (on a light [background]) or white (on a dark one) just far enough to
 * reach [minContrast] against it. A screen background that's built from the album's own dominant
 * colors can swallow those same colors when they're drawn on top of it, so raw swatches aren't safe
 * to draw with directly.
 */
internal fun readableOn(background: Color, color: Color, minContrast: Float = MIN_WAVE_CONTRAST): Color {
    if (contrastRatio(color, background) >= minContrast) return color
    val target = if (background.luminance() > 0.5f) Color.Black else Color.White
    var mix = 0f
    while (mix < 1f) {
        mix = (mix + 0.05f).coerceAtMost(1f)
        val candidate = lerp(color, target, mix)
        if (contrastRatio(candidate, background) >= minContrast) return candidate
    }
    return target
}

/**
 * A text color for a display drawn on [background]: the album's most dominant color, made readable
 * on it — or white while there are no [dominant] colors (the cover hasn't decoded, or Palette found
 * nothing).
 */
fun displayTextColor(dominant: List<Color>?, background: Color): Color =
    dominant?.firstOrNull()?.let { readableOn(background, it, MIN_TEXT_CONTRAST) } ?: Color.White

/**
 * Exactly [count] colors for [PreviewWaveform], each made readable on [background]: the album's
 * [dominant] colors, topped up from [fallback] (cycling if it's shorter than needed) when there are
 * fewer of them — [dominant] is `null` until the cover has decoded, and Palette can come up short.
 */
fun waveformColors(dominant: List<Color>?, fallback: List<Color>, background: Color, count: Int): List<Color> {
    require(count > 0) { "count must be positive" }
    require(fallback.isNotEmpty()) { "fallback must not be empty" }
    val fromAlbum = dominant.orEmpty().take(count).map { readableOn(background, it) }
    val topUp = List(count - fromAlbum.size) {
        readableOn(background, fallback[(fromAlbum.size + it) % fallback.size])
    }
    return fromAlbum + topUp
}
