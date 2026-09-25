package com.ruidoespontaneo.cassette.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.ruidoespontaneo.cassette.cover.components.TvStatic

/**
 * An album cover from [url], cropped to fill [modifier]'s bounds. A flat placeholder shows while it
 * loads, and TV static when there's no cover: the Cover Art Archive answers a 404 for albums without
 * one, which Coil reports as an error. [onSuccess] gets the loaded cover (for taking its colors).
 */
@Composable
fun CoverArt(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null
) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier = modifier) {
        val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            onLoading = { failed = false },
            onSuccess = {
                failed = false
                onSuccess?.invoke(it)
            },
            onError = { failed = true },
            modifier = Modifier.fillMaxSize()
        )
        if (failed) {
            TvStatic(Modifier.matchParentSize())
        }
    }
}
