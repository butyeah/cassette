package com.ruidoespontaneo.cassette.dayinhistory.presentation

import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album

/**
 * Cover Art Archive front-cover thumbnail for [this] album.
 *
 * Assumes [Album.id] is a MusicBrainz release-group MBID — true for every [Album] this screen
 * sees (they come from the Firestore day-index, whose document ids are release-group gids; see
 * DayInHistoryRepositoryImpl). Not necessarily true of [Album]s from other sources.
 */
fun Album.coverArtUrl(): String = "https://coverartarchive.org/release-group/$id/front-250"
