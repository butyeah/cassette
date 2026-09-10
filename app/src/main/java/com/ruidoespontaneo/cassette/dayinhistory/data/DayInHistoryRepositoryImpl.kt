package com.ruidoespontaneo.cassette.dayinhistory.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ruidoespontaneo.cassette.dayinhistory.domain.DayInHistoryRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class DayInHistoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DayInHistoryRepository {

    override suspend fun getAlbumsByDay(month: Int, day: Int): Result<List<Album>> {
        return try {
            val snapshot = firestore.collection(ALBUMS_BY_DAY_COLLECTION)
                .whereEqualTo(FIELD_MONTH, month)
                .whereEqualTo(FIELD_DAY, day)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toAlbum() })
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a query failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load albums for %02d-%02d", month, day)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toAlbum(): Album? {
        val title = getString(FIELD_TITLE) ?: return null
        val artistName = getString(FIELD_ARTIST_NAME) ?: return null
        val year = getLong(FIELD_YEAR)?.toInt() ?: return null
        val month = getLong(FIELD_MONTH)?.toInt() ?: return null
        val day = getLong(FIELD_DAY)?.toInt() ?: return null
        return Album(
            id = id,
            title = title,
            releaseDate = runCatching { LocalDate.of(year, month, day) }.getOrNull(),
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
