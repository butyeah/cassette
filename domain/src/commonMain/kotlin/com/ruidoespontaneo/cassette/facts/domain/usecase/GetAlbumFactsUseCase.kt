package com.ruidoespontaneo.cassette.facts.domain.usecase

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.facts.domain.AlbumFactsRepository
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

/**
 * Facts about [album] for its "About this album" card, named in [language] where possible; `null`
 * when there are none.
 */
class GetAlbumFactsUseCase @Inject constructor(
    private val albumFactsRepository: AlbumFactsRepository
) {
    // An album's id is its release-group MBID (see AlbumDetail), which is what Wikidata links to.
    suspend operator fun invoke(album: AlbumDetail, language: String): Result<AlbumFacts?> =
        albumFactsRepository.getAlbumFacts(releaseGroupId = album.id, language = language)
}
