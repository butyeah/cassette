package com.ruidoespontaneo.cassette.musicbrainz.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

/**
 * Cover Art Archive front-cover URL for [this] album, as shown on AlbumDetailScreen.
 *
 * Assumes [AlbumDetail.id] is a MusicBrainz release-group MBID — true for every [AlbumDetail]
 * this screen sees, since it's looked up by release-group id (see
 * [com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository.getAlbumDetail]).
 */
fun AlbumDetail.coverArtUrl(): String = "https://coverartarchive.org/release-group/$id/front-500"
