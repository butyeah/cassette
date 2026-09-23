package com.ruidoespontaneo.cassette.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private var pause: ImageVector? = null

/**
 * Material's "pause" glyph. It lives in `material-icons-extended`, a multi-megabyte dependency we'd
 * pull in for this one icon (the app only has `material-icons-core`), so it's drawn here from the
 * same path data instead.
 */
val Icons.Filled.Pause: ImageVector
    get() = pause ?: materialIcon(name = "Filled.Pause") {
        materialPath {
            moveTo(6f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineToRelative(14f)
            close()
            moveTo(14f, 5f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineToRelative(-4f)
            close()
        }
    }.also { pause = it }
