package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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

/**
 * "About this album": the lead of the album's Wikipedia article with a link to the rest, what
 * Wikidata knows as one labelled row per fact. Frosted over the album's gradient via [hazeState],
 * like the Daily screen's list cards. The sources are credited in Settings, on the Credits page.
 *
 * Under the title it's at most [COLLAPSED_HEIGHT] tall, fading out at the bottom over a chevron,
 * when there's more than fits. A tap anywhere but the Wikipedia link opens it to its full height,
 * pushing what's below down; another closes it. Content that fits is shown whole, with no chevron.
 */
@Composable
fun AboutAlbumCard(facts: AlbumFacts, hazeState: HazeState, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(facts) { mutableStateOf(false) }
    var fullHeight by remember { mutableIntStateOf(0) }
    val limit = with(LocalDensity.current) { COLLAPSED_HEIGHT.roundToPx() }
    val overflows = fullHeight > limit
    val fade by animateFloatAsState(if (overflows && !expanded) 1f else 0f, label = "aboutFade")
    val chevronTurn by animateFloatAsState(if (expanded) 180f else 0f, label = "aboutChevron")
    val toggleLabel = stringResource(if (expanded) R.string.about_album_show_less else R.string.about_album_show_more)
    CoverCard(hazeState = hazeState, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .then(if (overflows) Modifier.clickable(onClickLabel = toggleLabel) { expanded = !expanded } else Modifier)
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.about_album_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
                    .heightIn(max = if (expanded) Dp.Unspecified else COLLAPSED_HEIGHT)
                    .clipToBounds()
                    .fadeOutBottom(fade)
            ) {
                Column(
                    modifier = Modifier
                        // Laid out at its full height whatever the limit, to know whether it fits.
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .onSizeChanged { fullHeight = it.height },
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    facts.summary?.let { WikipediaSummary(it) }
                    facts.rows().forEach { (label, values) -> FactRow(label, values) }
                }
            }
            if (overflows) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null, // the card's click label says it
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .rotate(chevronTurn)
                )
            }
        }
    }
}

/** How tall the card's body is while closed. */
private val COLLAPSED_HEIGHT = 96.dp

/** How much of the bottom fades out while closed. */
private val FADE_HEIGHT = 32.dp

/**
 * Fades the bottom [FADE_HEIGHT] of this out to transparent, by [amount] (0 not at all, 1 fully).
 * A mask rather than a colour gradient, since the card is frosted glass with no colour of its own.
 */
private fun Modifier.fadeOutBottom(amount: Float): Modifier =
    if (amount == 0f) this else graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadeStart = size.height - FADE_HEIGHT.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black,
                    1f to Color.Black.copy(alpha = 1f - amount),
                    startY = fadeStart,
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
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
