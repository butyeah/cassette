package com.ruidoespontaneo.cassette.musicbrainz.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

/**
 * Cover Art Archive front-cover URL for a release group, at [size] pixels on its longest edge.
 *
 * Assumes the id is a MusicBrainz release-group MBID — true for every [Album]/[AlbumDetail] this
 * app deals with (see [Album.coverArtUrl] and [AlbumDetail.coverArtUrl]).
 */
private fun coverArtArchiveUrl(releaseGroupId: String, size: Int): String =
    "https://coverartarchive.org/release-group/$releaseGroupId/front-$size"

/**
 * Thumbnail-sized cover art for [this] album, as shown in a list row (e.g. the Daily screen's
 * AlbumRow). [Album.id] comes from the Firestore day-index, whose document ids are release-group
 * gids — see DayInHistoryRepositoryImpl. Not necessarily true of [Album]s from other sources.
 */
fun Album.coverArtUrl(): String = coverArtArchiveUrl(id, size = 250)

/** Larger, hero-sized cover art for [this] album, as shown on AlbumDetailScreen. */
fun AlbumDetail.coverArtUrl(): String = coverArtArchiveUrl(id, size = 500)
