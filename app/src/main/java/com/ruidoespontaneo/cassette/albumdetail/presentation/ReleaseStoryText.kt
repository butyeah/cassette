package com.ruidoespontaneo.cassette.albumdetail.presentation

import android.content.res.Resources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory
import com.ruidoespontaneo.cassette.facts.domain.model.ReleaseStory.Phrase
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.toJavaLocalDate

/**
 * This story as a sentence in the app's language, with what changes from album to album (the date
 * and every name) in bold. Every phrase string takes the same arguments, using whichever it needs:
 * 1 the full date ("21 de mayo de 1997"), 2 the labels, 3 the producers, 4 the artist.
 */
fun ReleaseStory.text(resources: Resources): AnnotatedString {
    val locale = resources.configuration.locales[0]
    val date = date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    val and = resources.getString(R.string.list_and)
    return boldArguments(
        pattern = resources.getString(phrase.stringRes()),
        arguments = listOf(listOf(date), labels, producers, listOf(artist)),
        and = and
    )
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

private val argument = Regex("%(\\d+)\\\$s")

/**
 * [pattern] ("El %1$s, el sello %2$s lanzó este disco.") with each `%n$s` replaced by the names in
 * [arguments]' n-th list, joined by [joinNames] with [and], each name in bold and the joining words
 * not.
 */
internal fun boldArguments(pattern: String, arguments: List<List<String>>, and: String): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (match in argument.findAll(pattern)) {
            append(pattern.substring(last, match.range.first))
            appendNames(arguments[match.groupValues[1].toInt() - 1], and)
            last = match.range.last + 1
        }
        append(pattern.substring(last))
    }

/** "A", "A and B", "A, B and C", with the names in bold. */
private fun AnnotatedString.Builder.appendNames(names: List<String>, and: String) {
    fun bold(name: String) = withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
    if (names.size < 2) {
        names.firstOrNull()?.let(::bold)
        return
    }
    // [and] is "%1$s and %2$s": everything but the last name goes in the first slot.
    val (before, between, after) = and.split("%1\$s", "%2\$s")
    append(before)
    names.dropLast(1).forEachIndexed { index, name ->
        if (index > 0) append(", ")
        bold(name)
    }
    append(between)
    bold(names.last())
    append(after)
}
