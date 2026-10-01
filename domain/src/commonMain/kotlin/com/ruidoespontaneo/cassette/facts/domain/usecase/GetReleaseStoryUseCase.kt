package com.ruidoespontaneo.cassette.facts.domain.usecase

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory.Phrase
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail

/**
 * Decides which sentence tells how [AlbumDetail] came out: whether its [AlbumFacts] name a label,
 * and whether it was produced by someone else, by the artist themself, or by several people.
 *
 * `null` when the album has no precise release date, so there's nothing to tell. That can't happen
 * for an album opened from Daily, which only lists albums with one. [facts] is `null` when Wikidata
 * knows nothing, which gives [Phrase.DateOnly].
 */
class GetReleaseStoryUseCase @Inject constructor() {

    operator fun invoke(album: AlbumDetail, facts: AlbumFacts?): ReleaseStory? {
        val date = album.firstReleaseDate ?: return null
        val labels = facts?.labels.orEmpty()
        val producers = facts?.producers.orEmpty()
        val production = when {
            producers.isEmpty() -> Production.None
            producers.size > 1 -> Production.Several
            sameName(producers.single(), album.artistName) -> Production.Self
            else -> Production.Other
        }
        val phrase = if (labels.isNotEmpty()) {
            when (production) {
                Production.Other -> Phrase.LabelAndProducer
                Production.Self -> Phrase.LabelSelfProduced
                Production.Several -> Phrase.LabelAndProducers
                Production.None -> Phrase.LabelOnly
            }
        } else {
            when (production) {
                Production.Other -> Phrase.Producer
                Production.Self -> Phrase.SelfProduced
                Production.Several -> Phrase.Producers
                Production.None -> Phrase.DateOnly
            }
        }
        return ReleaseStory(phrase, date, album.artistName, labels, producers)
    }

    private enum class Production { None, Other, Self, Several }
}

/**
 * Whether two names are the same once case, accents and spacing are set aside: Wikidata's producer
 * "Björk" is MusicBrainz's artist "Bjork". A small fold of Latin accents rather than Unicode
 * normalization, which common code has no library for.
 */
internal fun sameName(a: String, b: String): Boolean = a.foldedName() == b.foldedName()

private fun String.foldedName(): String =
    lowercase()
        .map { accentFolds[it] ?: it }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), " ")

private val accentFolds: Map<Char, Char> = buildMap {
    fun fold(accented: String, plain: Char) = accented.forEach { put(it, plain) }
    fold("àáâãäåā", 'a')
    fold("çćč", 'c')
    fold("èéêëēė", 'e')
    fold("ìíîïī", 'i')
    fold("ñń", 'n')
    fold("òóôõöøō", 'o')
    fold("śšß", 's')
    fold("ùúûüū", 'u')
    fold("ýÿ", 'y')
    fold("žźż", 'z')
}
