package com.ruidoespontaneo.cassette.cover.components

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay

private const val FRAME_INTERVAL_MS = 1000L / 12

/** Made once, on first use, and shared by every [TvStatic] on screen. */
private val tvStaticBitmaps: List<ImageBitmap> by lazy {
    tvStaticFrames().map { levels ->
        Bitmap.createBitmap(
            levels.toArgbPixels(),
            TV_STATIC_FRAME_SIZE,
            TV_STATIC_FRAME_SIZE,
            Bitmap.Config.ARGB_8888
        ).asImageBitmap()
    }
}

/**
 * Animated TV snow, for an album with no cover. It cycles a few pre-made frames ([tvStaticFrames])
 * at 12 fps, so a grid full of missing covers costs one image swap per tick. It holds still while
 * the system's animations are off. Fills its bounds; decorative, so it has no semantics.
 */
@Composable
fun TvStatic(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animationsOff = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(animationsOff) {
        if (animationsOff) return@LaunchedEffect
        while (true) {
            delay(FRAME_INTERVAL_MS)
            frame = (frame + 1) % TV_STATIC_FRAME_COUNT
        }
    }
    Canvas(modifier) {
        drawImage(
            image = tvStaticBitmaps[frame],
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(TV_STATIC_FRAME_SIZE, TV_STATIC_FRAME_SIZE),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            // Blocky, like a CRT's snow, rather than smoothed into a blur.
            filterQuality = FilterQuality.None
        )
    }
}
