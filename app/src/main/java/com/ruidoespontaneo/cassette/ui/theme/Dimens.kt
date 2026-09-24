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
    /** The play/pause status glyph beside the preview display's "Track n" label. */
    val previewStatus = 16.dp
    /** Material's minimum touch target — the size of an IconButton, e.g. to balance one with a spacer. */
    val minTouchTarget = 48.dp
    /**
     * Every tracklist row's height, whether or not it has a play button — the button's touch
     * target — so rows are evenly spaced and the active-track highlight never changes size.
     */
    val trackRow = minTouchTarget
    /** The now-playing dialog's play/stop button. */
    val nowPlayingButton = 64.dp
    /** The cover thumbnail on the floating toolbar's now-playing button. */
    val nowPlayingThumbnail = 24.dp
    /** The signed-in user's initial on the Profile tab. */
    val avatar = 64.dp
}

/** Space the floating bottom toolbar occupies; scrolling top-level screens pad their end by this so nothing hides under it. */
object ToolbarSize {
    val clearance = 88.dp
}
