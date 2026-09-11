package com.ruidoespontaneo.cassette.musicbrainz.domain.usecase

import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import javax.inject.Inject

/** Full detail for one album, looked up by its MusicBrainz id — see [MusicBrainzRepository.getAlbumDetail]. */
class GetAlbumDetailUseCase @Inject constructor(
    private val repository: MusicBrainzRepository
) {
    suspend operator fun invoke(id: String): Result<AlbumDetail> = repository.getAlbumDetail(id)
}
