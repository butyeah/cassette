package com.ruidoespontaneo.cassette.facts.data

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.core.logging.logError
import com.ruidoespontaneo.cassette.facts.data.api.WikidataApi
import com.ruidoespontaneo.cassette.facts.data.api.WikipediaApi
import com.ruidoespontaneo.cassette.facts.domain.AlbumFactsRepository
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How many albums [WikidataAlbumFactsRepository] remembers, so paging back and forth is free. */
private const val CACHE_SIZE = 50

/** The most labels [WikidataApi.labels] takes in one call. */
private const val MAX_LABELS = 50

private const val PRODUCER = "P162"
private const val RECORDED_AT = "P483"
private const val COVER_ART_BY = "P736"
private const val RECORD_LABEL = "P264"
private const val AWARD_RECEIVED = "P166"
private const val NOMINATED_FOR = "P1411"

/**
 * [AlbumFactsRepository] over Wikidata and Wikipedia: finds the album's item by its MusicBrainz id,
 * reads the claims the card shows and its Wikipedia articles' titles, then names every item the
 * claims point to in one call, in the user's language or else English. The summary is the lead of
 * the article in that same order of languages.
 */
class WikidataAlbumFactsRepository @Inject constructor(
    private val api: WikidataApi,
    private val wikipedia: WikipediaApi
) : AlbumFactsRepository {

    private data class Key(val releaseGroupId: String, val language: String)

    // Oldest first. Holds found-nothing too (as null); failures aren't kept, so they're retried.
    private val cache = LinkedHashMap<Key, AlbumFacts?>()
    private val cacheLock = Mutex()

    override suspend fun getAlbumFacts(releaseGroupId: String, language: String): Result<AlbumFacts?> {
        val key = Key(releaseGroupId, language)
        cacheLock.withLock { if (key in cache) return Result.success(cache[key]) }
        return try {
            val facts = lookUp(releaseGroupId, language)
            cacheLock.withLock {
                cache[key] = facts
                if (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
            }
            Result.success(facts)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            logError(e, "Failed to load Wikidata facts for $releaseGroupId")
            Result.failure(e)
        }
    }

    private suspend fun lookUp(releaseGroupId: String, language: String): AlbumFacts? {
        val itemId = api.findItem(releaseGroupId) ?: return null
        val languages = listOf(language, "en").distinct()
        val item = api.item(itemId, languages)
        val summary = summary(item.articleTitles, languages, releaseGroupId)
        val shown = listOf(PRODUCER, RECORDED_AT, COVER_ART_BY, RECORD_LABEL, AWARD_RECEIVED, NOMINATED_FOR)
        val ids = shown.flatMap { item.claims[it].orEmpty() }.distinct().take(MAX_LABELS)

        val names = if (ids.isEmpty()) emptyMap() else api.labels(ids, languages)
        fun named(property: String) = item.claims[property].orEmpty().mapNotNull { names[it] }.distinct()
        return AlbumFacts(
            producers = named(PRODUCER),
            recordedAt = named(RECORDED_AT),
            coverArtBy = named(COVER_ART_BY),
            labels = named(RECORD_LABEL),
            awards = named(AWARD_RECEIVED),
            nominations = named(NOMINATED_FOR),
            summary = summary
        ).takeUnless { it.isEmpty }
    }

    /**
     * The lead of the first of [languages] that has an article in [articleTitles]. Failing to load
     * it leaves the summary out rather than failing the whole lookup: the Wikidata facts still show.
     */
    private suspend fun summary(articleTitles: Map<String, String>, languages: List<String>, releaseGroupId: String): AlbumSummary? {
        val language = languages.firstOrNull { it in articleTitles } ?: return null
        return try {
            wikipedia.summary(language, articleTitles.getValue(language))?.let { AlbumSummary(it.extract, it.pageUrl) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(e, "Failed to load the Wikipedia summary for $releaseGroupId")
            null
        }
    }
}
