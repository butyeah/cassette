package com.ruidoespontaneo.cassette.cover.components

import android.graphics.Bitmap
import android.os.Build
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
    // Image loaders (Coil included) decode into a Bitmap.Config.HARDWARE bitmap by default for
    // performance — it lives in GPU memory and Palette (like anything calling getPixels())
    // crashes on it with "pixel access is not supported on Config#HARDWARE bitmaps". Copy to a
    // readable config first; HARDWARE itself doesn't exist before API 26, so nothing below that
    // can ever be one.
    val readableBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this@dominantColors
    }
    Palette.Builder(readableBitmap).generate()
        .swatches
        .sortedByDescending { it.population }
        .take(count)
        .map { Color(it.rgb) }
}
