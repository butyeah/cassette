package com.ruidoespontaneo.cassette.itunes.data

/** The album an Apple Music link points at, and the storefront the link was made for. */
internal data class AppleMusicAlbumRef(val collectionId: Long, val storefront: String?)

// https://music.apple.com/us/album/in-between-dreams/1440857781 (optionally ?i=<songId>), or the
// slug-less https://music.apple.com/us/album/1440857781.
private val APPLE_MUSIC_ALBUM = Regex("""^https?://music\.apple\.com/(?:([a-z]{2})/)?album/(?:[^/?#]+/)?(?:id)?(\d+)""")

/** `null` when [url] isn't an Apple Music album link. */
internal fun parseAppleMusicAlbumLink(url: String): AppleMusicAlbumRef? {
    val match = APPLE_MUSIC_ALBUM.find(url) ?: return null
    val collectionId = match.groupValues[2].toLongOrNull() ?: return null
    return AppleMusicAlbumRef(collectionId, storefront = match.groupValues[1].ifEmpty { null })
}
