package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.components.CoverCard
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumSummary
import dev.chrisbanes.haze.HazeState

/** Each of [AlbumFacts]' rows with its label, in the card's order, leaving out the empty ones. */
private fun AlbumFacts.rows(): List<Pair<Int, List<String>>> = listOf(
    R.string.facts_produced_by to producers,
    R.string.facts_recorded_at to recordedAt,
    R.string.facts_cover_art_by to coverArtBy,
    R.string.facts_label to labels,
    R.string.facts_awards to awards,
    R.string.facts_nominations to nominations
).filter { (_, values) -> values.isNotEmpty() }

/** Credits whichever of Wikidata and Wikipedia the card shows something from. */
@StringRes
private fun AlbumFacts.creditRes(): Int {
    val hasWikidataFacts = rows().isNotEmpty()
    return when {
        summary == null -> R.string.facts_credit
        hasWikidataFacts -> R.string.facts_credit_both
        else -> R.string.facts_credit_wikipedia
    }
}

/**
 * "About this album": the lead of the album's Wikipedia article with a link to the rest, what
 * Wikidata knows as one labelled row per fact, then the credit for both. Frosted over the album's
 * gradient via [hazeState], like the Daily screen's list cards.
 */
@Composable
fun AboutAlbumCard(facts: AlbumFacts, hazeState: HazeState, modifier: Modifier = Modifier) {
    CoverCard(hazeState = hazeState, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.about_album_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            facts.summary?.let { WikipediaSummary(it) }
            facts.rows().forEach { (label, values) -> FactRow(label, values) }
            Text(
                text = stringResource(facts.creditRes()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The article's lead, then a link to the whole article, which Wikipedia's license asks for. */
@Composable
private fun WikipediaSummary(summary: AlbumSummary) {
    val uriHandler = LocalUriHandler.current
    Column {
        Text(text = summary.text, style = MaterialTheme.typography.bodyMedium)
        TextButton(
            onClick = { uriHandler.openUri(summary.articleUrl) },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(stringResource(R.string.facts_read_on_wikipedia))
        }
    }
}

@Composable
private fun FactRow(@StringRes label: Int, values: List<String>) {
    Column {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = values.joinToString(), style = MaterialTheme.typography.bodyMedium)
    }
}
