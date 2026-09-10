package com.ruidoespontaneo.cassette.musicbrainz.data

import com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ArtistDto
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Artist
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class MusicBrainzRepositoryImpl @Inject constructor(
    private val api: MusicBrainzApi
) : MusicBrainzRepository {

    override suspend fun searchArtists(query: String, limit: Int, offset: Int): Result<List<Artist>> {
        return try {
            val artists = api.searchArtists(query = query, limit = limit, offset = offset)
                .artists
                .map { it.toDomain() }
            Result.success(artists)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a search failure.
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ArtistDto.toDomain() = Artist(
        id = id,
        name = name,
        sortName = sortName,
        type = type,
        country = country,
        disambiguation = disambiguation,
        score = score?.toIntOrNull()
    )
}
