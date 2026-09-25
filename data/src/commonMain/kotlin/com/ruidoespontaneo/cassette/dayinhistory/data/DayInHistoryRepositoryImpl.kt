package com.ruidoespontaneo.cassette.dayinhistory.data

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.core.firestore.FirestoreDocument
import com.ruidoespontaneo.cassette.core.firestore.FirestoreDocuments
import com.ruidoespontaneo.cassette.core.firestore.int
import com.ruidoespontaneo.cassette.core.firestore.string
import com.ruidoespontaneo.cassette.core.logging.logError
import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

class DayInHistoryRepositoryImpl @Inject constructor(
    private val firestore: FirestoreDocuments
) : DayInHistoryRepository {

    override suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>> {
        return try {
            val documents = firestore.query(
                ALBUMS_BY_DAY_COLLECTION,
                fieldsEqualTo = mapOf(FIELD_MONTH to month, FIELD_DAY to day)
            )
            Result.success(documents.mapNotNull { it.toAlbum() })
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a query failure.
            throw e
        } catch (e: Exception) {
            logError(e, "Failed to load albums for $month-$day")
            Result.failure(e)
        }
    }

    private fun FirestoreDocument.toAlbum(): Album? {
        val title = fields.string(FIELD_TITLE) ?: return null
        val artistName = fields.string(FIELD_ARTIST_NAME) ?: return null
        val year = fields.int(FIELD_YEAR) ?: return null
        val month = fields.int(FIELD_MONTH) ?: return null
        val day = fields.int(FIELD_DAY) ?: return null
        return Album(
            id = id,
            title = title,
            releaseDate = runCatching { LocalDate(year, month, day) }.getOrNull(),
            // The offline pipeline doesn't upload a MusicBrainz artist id, only its name.
            artistId = null,
            artistName = artistName
        )
    }

    private companion object {
        const val ALBUMS_BY_DAY_COLLECTION = "albumsByDay"
        const val FIELD_TITLE = "title"
        const val FIELD_ARTIST_NAME = "artistName"
        const val FIELD_YEAR = "year"
        const val FIELD_MONTH = "month"
        const val FIELD_DAY = "day"
    }
}
