package com.ruidoespontaneo.cassette.itunes.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppleMusicLinkTest {

    @Test
    fun `parses a slug link with its storefront`() {
        assertEquals(
            AppleMusicAlbumRef(collectionId = 1440857781, storefront = "us"),
            parseAppleMusicAlbumLink("https://music.apple.com/us/album/in-between-dreams/1440857781")
        )
    }

    @Test
    fun `parses a slug-less link`() {
        assertEquals(
            AppleMusicAlbumRef(collectionId = 1440857781, storefront = "gb"),
            parseAppleMusicAlbumLink("https://music.apple.com/gb/album/1440857781")
        )
    }

    @Test
    fun `ignores the song query parameter`() {
        assertEquals(
            1440857781L,
            parseAppleMusicAlbumLink("https://music.apple.com/us/album/in-between-dreams/1440857781?i=1440857800")
                ?.collectionId
        )
    }

    @Test
    fun `parses an id-prefixed link`() {
        assertEquals(
            123L,
            parseAppleMusicAlbumLink("https://music.apple.com/us/album/some-album/id123")?.collectionId
        )
    }

    @Test
    fun `has no storefront when the link has none`() {
        assertNull(parseAppleMusicAlbumLink("https://music.apple.com/album/some-album/123")?.storefront)
    }

    @Test
    fun `rejects links that are not Apple Music albums`() {
        assertNull(parseAppleMusicAlbumLink("https://music.apple.com/us/artist/muse/12345"))
        assertNull(parseAppleMusicAlbumLink("https://open.spotify.com/album/abc"))
        assertNull(parseAppleMusicAlbumLink("not a url"))
    }
}
