package com.ruidoespontaneo.cassette.albumdetail.presentation

import android.content.res.Resources
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory.Phrase
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.toJavaLocalDate

/**
 * This story as a sentence in the app's language. Every phrase string takes the same arguments,
 * using whichever it needs: 1 the full date ("21 de mayo de 1997"), 2 the labels, 3 the producers,
 * 4 the artist.
 */
fun ReleaseStory.text(resources: Resources): String {
    val locale = resources.configuration.locales[0]
    val date = date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    val and = resources.getString(R.string.list_and)
    return resources.getString(phrase.stringRes(), date, joinNames(labels, and), joinNames(producers, and), artist)
}

private fun Phrase.stringRes(): Int = when (this) {
    Phrase.LabelAndProducer -> R.string.release_story_label_and_producer
    Phrase.LabelSelfProduced -> R.string.release_story_label_self_produced
    Phrase.LabelAndProducers -> R.string.release_story_label_and_producers
    Phrase.LabelOnly -> R.string.release_story_label_only
    Phrase.Producer -> R.string.release_story_producer
    Phrase.SelfProduced -> R.string.release_story_self_produced
    Phrase.Producers -> R.string.release_story_producers
    Phrase.DateOnly -> R.string.release_story_date_only
}

/**
 * Every name in [names] as one: "A", "A and B", "A, B and C". [and] is the `list_and` pattern,
 * "%1$s and %2$s", joining everything but the last name to it. Done by hand because Android's
 * ListFormatter needs API 26 and minSdk is 24.
 */
internal fun joinNames(names: List<String>, and: String): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> and.format(names.dropLast(1).joinToString(", "), names.last())
}
