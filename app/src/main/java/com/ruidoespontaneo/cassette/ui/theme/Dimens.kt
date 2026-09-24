package com.ruidoespontaneo.cassette.ui.theme

import androidx.compose.ui.unit.dp

/** Fixed sizes for image/icon elements. */
object IconSize {
    val albumArt = 48.dp
    /** Hero-sized cover art, e.g. on AlbumDetailScreen. */
    val albumArtLarge = 200.dp
    /** The loading spinner that stands in for a track row's play button while its preview buffers. */
    val previewSpinner = 20.dp
    val previewSpinnerStroke = 2.dp
    /** The play/pause/stop status glyph beside the preview display's "Track n" label. */
    val previewStatus = 16.dp
}

/** Space the floating bottom toolbar occupies; scrolling top-level screens pad their end by this so nothing hides under it. */
object ToolbarSize {
    val clearance = 88.dp
}
