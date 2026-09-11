package com.ruidoespontaneo.cassette.cover.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The [count] most dominant colors in this bitmap, most-populous swatch first — meant to feed
 * [AnimatedGradientBackground]'s `colors` param so a screen's background can take on the colors
 * of whatever image it's showing (e.g. an album's cover art).
 *
 * Runs off the main thread: [Palette.Builder.generate] is synchronous CPU work, and shouldn't run
 * during composition/on the UI thread even for a small, already-downsampled bitmap.
 */
suspend fun Bitmap.dominantColors(count: Int = 3): List<Color> = withContext(Dispatchers.Default) {
    Palette.Builder(this@dominantColors).generate()
        .swatches
        .sortedByDescending { it.population }
        .take(count)
        .map { Color(it.rgb) }
}
