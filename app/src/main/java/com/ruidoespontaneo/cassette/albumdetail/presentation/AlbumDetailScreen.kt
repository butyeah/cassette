package com.ruidoespontaneo.cassette.albumdetail.presentation

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.ui.components.CoverArt
import coil3.toBitmap
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.components.AnimatedGradientBackground
import com.ruidoespontaneo.cassette.cover.components.ContainerColors
import com.ruidoespontaneo.cassette.cover.components.PREVIEW_WAVEFORM_LINES
import com.ruidoespontaneo.cassette.cover.components.PreviewWaveform
import com.ruidoespontaneo.cassette.cover.components.displayTextColor
import com.ruidoespontaneo.cassette.cover.components.dominantColors
import com.ruidoespontaneo.cassette.cover.components.highlightContainerColors
import com.ruidoespontaneo.cassette.cover.components.rememberWavePhase
import com.ruidoespontaneo.cassette.cover.components.waveformColors
import com.ruidoespontaneo.cassette.cover.components.wavyPillBackground
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.facts.domain.model.AlbumFacts
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import com.ruidoespontaneo.cassette.musicbrainz.presentation.asDisplayList
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.musicbrainz.presentation.durationText
import com.ruidoespontaneo.cassette.musicbrainz.presentation.hasAny
import com.ruidoespontaneo.cassette.ui.icons.Pause
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import com.ruidoespontaneo.cassette.ui.theme.IconSize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * [viewModel] has no default — it's assisted-injected per album (see [AlbumDetailViewModel]), so
 * the caller must build it via `hiltViewModel`'s assisted-injection overload, keyed by albumId.
 */
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlbumDetailScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailScreenContent(
    state: AlbumDetailUiState,
    onIntent: (AlbumDetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = rememberHazeState()
    val scrollState = rememberScrollState()
    // Reveal the title once the header (cover art) has scrolled out of view, so it's still clear
    // which album this is deep in a long tracklist — this screen has no visible bar chrome to
    // collapse into, so a title fade-in stands in for the usual Material collapsing app bar.
    val revealThresholdPx = with(LocalDensity.current) { IconSize.albumArtLarge.toPx() }
    val showTitle by remember { derivedStateOf { scrollState.value > revealThresholdPx } }
    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        label = "titleAlpha"
    )

    // The background takes on the cover art's own dominant colors once it's decoded — falls back
    // to AnimatedGradientBackground's theme-colored default (null) until then, or if extraction
    // comes up short.
    var coverBitmap by remember(state.album?.id) { mutableStateOf<Bitmap?>(null) }
    var dominantColors by remember(state.album?.id) { mutableStateOf<List<Color>?>(null) }
    LaunchedEffect(coverBitmap) { dominantColors = coverBitmap?.dominantColors() }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedGradientBackground(
            Modifier
                .matchParentSize()
                .hazeSource(hazeState),
            colors = dominantColors
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.album?.title.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(titleAlpha)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            when {
                state.isLoading -> LoadingIndicator(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )

                state.errorRes != null -> ErrorMessage(
                    message = stringResource(state.errorRes),
                    onRetry = { onIntent(AlbumDetailIntent.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )

                state.album != null -> AlbumDetailContent(
                    album = state.album,
                    previews = state.previews,
                    previewPlayback = state.previewPlayback,
                    onTogglePreview = { onIntent(AlbumDetailIntent.TogglePreview(it)) },
                    scrollState = scrollState,
                    waveColors = dominantColors,
                    onCoverLoaded = { coverBitmap = it },
                    facts = state.facts,
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun AlbumDetailContent(
    album: AlbumDetail,
    previews: Map<Int, String>,
    previewPlayback: TrackPlayback?,
    onTogglePreview: (trackPosition: Int) -> Unit,
    scrollState: ScrollState,
    /** The album's dominant colors once its cover has decoded; the waveform falls back to theme colors until then. */
    waveColors: List<Color>?,
    onCoverLoaded: (Bitmap) -> Unit,
    /** Shown in a card after the tracklist when there are any. */
    facts: AlbumFacts?,
    /** The background's, so the facts card can frost it. */
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(Spacing.large)
    ) {
        Text(
            text = album.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = Spacing.medium)
        )
        Text(text = album.artistName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.medium))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Spacing.small))
                .background(color = Color.Black),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // A player-style display panel beside the cover: track number and remaining time across
            // the top, the waveform along the bottom.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(IconSize.albumArtLarge)
                    .clip(RoundedCornerShape(Spacing.small))
                    .background(Color.Black),
                contentAlignment = Alignment.BottomCenter
            ) {
                PreviewDisplayReadout(
                    playback = previewPlayback,
                    title = previewTrackTitle(album.tracks, previewPlayback),
                    color = displayTextColor(dominant = waveColors, background = Color.Black),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                )
                PreviewWaveform(
                    playing = previewPlayback != null && !previewPlayback.isLoading,
                    colors = waveformColors(
                        dominant = waveColors,
                        fallback = with(MaterialTheme.colorScheme) {
                            listOf(
                                primary,
                                secondary,
                                tertiary
                            )
                        },
                        background = Color.Black,
                        count = PREVIEW_WAVEFORM_LINES
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.medium,
                            end = Spacing.medium,
                            bottom = Spacing.medium
                        )
                )
            }
            CoverArt(
                url = album.coverArtUrl(),
                contentDescription = null, // decorative — title/artist are already read by the screen
                onSuccess = { onCoverLoaded(it.result.image.toBitmap()) },
                modifier = Modifier
                    .size(IconSize.albumArtLarge)
                    .clip(RoundedCornerShape(Spacing.small))
            )
        }
        if (album.streamingLinks.hasAny()) {
            StreamingLinksRow(
                album.streamingLinks,
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
        AlbumTypeAndYear(album, modifier = Modifier.padding(top = Spacing.medium))
        if (album.genres.isNotEmpty()) {
            Text(
                text = stringResource(R.string.genres_format, album.genres.joinToString()),
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
        val ratingValue = album.ratingValue
        if (ratingValue != null) {
            Text(
                text = pluralStringResource(
                    R.plurals.rating_format,
                    album.ratingVotesCount,
                    ratingValue,
                    album.ratingVotesCount
                ),
                modifier = Modifier.padding(top = Spacing.small)
            )
        }
        if (album.tracks.isNotEmpty()) {
            Tracklist(
                tracks = album.tracks,
                previews = previews,
                previewPlayback = previewPlayback,
                highlightColors = highlightContainerColors(waveColors),
                onTogglePreview = onTogglePreview,
                modifier = Modifier.padding(top = Spacing.large)
            )
        }
        if (facts != null) {
            AboutAlbumCard(facts = facts, hazeState = hazeState, modifier = Modifier.padding(top = Spacing.large))
        }
    }
}

/**
 * "Track n" and the clip's remaining time, with the track's [title] in a box under them — dashes for
 * all three while no preview is playing. The box is filled with [color] so it stands out on the
 * black display; a title too long for it scrolls across every [TITLE_SCROLL_INTERVAL_MS].
 */
@Composable
private fun PreviewDisplayReadout(
    playback: TrackPlayback?,
    title: String?,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Tabular figures keep the countdown's digits from shifting as they change.
    val style =
        MaterialTheme.typography.labelLarge.copy(color = color, fontFeatureSettings = "tnum")
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewStatusIcon(status = previewStatus(playback), tint = color)
                Text(
                    text = if (playback == null) {
                        stringResource(R.string.preview_track_idle)
                    } else {
                        stringResource(R.string.preview_track_number, playback.position)
                    },
                    style = style,
                    modifier = Modifier.padding(start = Spacing.extraSmall)
                )
            }
            Text(text = remainingTimeText(playback?.remainingMs), style = style)
        }
        Box(
            modifier = Modifier
                .padding(top = Spacing.small)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Spacing.extraSmall))
                .background(color)
                .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall)
        ) {
            // Keyed so the marquee starts over, from its initial delay, whenever the track changes.
            key(title) {
                Text(
                    text = title ?: stringResource(R.string.preview_title_idle),
                    // [color] is readable on black, so black is readable on [color].
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.Black),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = TITLE_SCROLL_INTERVAL_MS,
                        repeatDelayMillis = TITLE_SCROLL_INTERVAL_MS
                    )
                )
            }
        }
    }
}

private const val TITLE_SCROLL_INTERVAL_MS = 10_000

/** Media-control style: ▶ while stopped, ❚❚ while a clip is buffering or playing. */
@Composable
private fun PreviewStatusIcon(status: PreviewStatus, tint: Color, modifier: Modifier = Modifier) {
    Crossfade(
        targetState = status,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "previewStatusIcon",
        modifier = modifier
    ) { shown ->
        val (icon, description) = when (shown) {
            PreviewStatus.Stopped -> Icons.Filled.PlayArrow to R.string.preview_status_stopped
            PreviewStatus.Playing -> Icons.Filled.Pause to R.string.preview_status_playing
        }
        Icon(
            imageVector = icon,
            contentDescription = stringResource(description),
            tint = tint,
            modifier = Modifier.size(IconSize.previewStatus)
        )
    }
}

@Composable
private fun Tracklist(
    tracks: List<Track>,
    previews: Map<Int, String>,
    previewPlayback: TrackPlayback?,
    /** The playing track's highlight, from the album's dominant colors; `null` falls back to the theme. */
    highlightColors: ContainerColors?,
    onTogglePreview: (trackPosition: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePosition = previewPlayback?.position
    // Animated so the highlight eases over when the cover finishes decoding mid-playback (the row's
    // content color animates on its own, in TrackRow).
    val highlightContainer by animateColorAsState(
        targetValue = highlightColors?.container ?: MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "highlightContainer"
    )
    val highlightContent = highlightColors?.content ?: MaterialTheme.colorScheme.onSecondaryContainer
    // Where each row sits inside the Box below, so one shared indicator can travel between them.
    val rowBounds = remember { mutableStateMapOf<Int, RowBounds>() }
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.tracklist_title),
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            modifier = Modifier
                .padding(top = Spacing.small)
                .clip(RoundedCornerShape(Spacing.small))
        ) {
            ActiveTrackIndicator(
                target = activePosition?.let { rowBounds[it] },
                playing = previewPlayback?.isLoading == false,
                color = highlightContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                tracks.forEach { track ->
                    TrackRow(
                        track = track,
                        hasPreview = track.position in previews,
                        playback = previewPlayback?.takeIf { it.position == track.position },
                        activeContentColor = highlightContent,
                        onTogglePreview = { onTogglePreview(track.position) },
                        modifier = Modifier.onPlaced { coordinates ->
                            rowBounds[track.position] = RowBounds(
                                top = coordinates.positionInParent().y,
                                height = coordinates.size.height.toFloat()
                            )
                        }
                    )
                }
            }
        }
    }
}

/** A track row's offset and height inside [Tracklist]'s Box, in px. */
private data class RowBounds(val top: Float, val height: Float)

/**
 * The pill behind the track that's playing — M3 Expressive's active indicator. It's one indicator
 * for the whole tracklist rather than one per row, so moving between tracks (a tap, or autoplay
 * advancing) slides it across on a spatial spring that overshoots and settles. Appearing and
 * disappearing fade in place instead: sliding in from wherever it last was would read as noise.
 *
 * Its top and bottom edges ripple like the preview display's waveform while the clip is audible
 * ([playing]), and ease flat while it buffers or once it stops.
 */
@Composable
private fun ActiveTrackIndicator(
    target: RowBounds?,
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val motion = MaterialTheme.motionScheme
    val waveAmplitude = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        waveAmplitude.animateTo(if (playing) 1f else 0f, motion.defaultSpatialSpec())
    }
    val wavePhase = rememberWavePhase(running = playing, isSettling = { waveAmplitude.value > 0f })
    val top = remember { Animatable(0f) }
    val height = remember { Animatable(0f) }
    var isShown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (target != null) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec(),
        label = "indicatorAlpha"
    )
    LaunchedEffect(target) {
        if (target == null) {
            isShown = false
        } else if (!isShown) {
            top.snapTo(target.top)
            height.snapTo(target.height)
            isShown = true
        } else {
            launch { top.animateTo(target.top, motion.defaultSpatialSpec()) }
            launch { height.animateTo(target.height, motion.defaultSpatialSpec()) }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = top.value
                this.alpha = alpha
            }
            .layout { measurable, constraints ->
                val px = height.value.roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(constraints.copy(minHeight = px, maxHeight = px))
                layout(placeable.width, px) { placeable.place(0, 0) }
            }
            .wavyPillBackground(
                color = color,
                amplitude = { waveAmplitude.value },
                phase = { wavePhase.floatValue }
            )
    )
}

/** [playback] is non-null only for the one track whose preview is buffering or playing. */
@Composable
private fun TrackRow(
    track: Track,
    hasPreview: Boolean,
    playback: TrackPlayback?,
    /** Text and icon color while this row sits on the active-track highlight. */
    activeContentColor: Color,
    onTogglePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = playback != null
    val contentColor by animateColorAsState(
        targetValue = if (isActive) activeContentColor else LocalContentColor.current,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "trackContentColor"
    )
    // Autoplay can move on to a track that's scrolled out of view — follow it.
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(isActive) {
        if (isActive) bringIntoViewRequester.bringIntoView()
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IconSize.trackRow)
                .bringIntoViewRequester(bringIntoViewRequester)
                .padding(start = Spacing.large, end = Spacing.extraSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // One line, so a long title can't make its row taller than the rest.
            Text(
                text = "${track.position}. ${track.title}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            val durationText = track.durationText()
            if (durationText != null) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.small)
                )
            }
            if (hasPreview) {
                PreviewButton(track.title, playback, onTogglePreview)
            } else {
                // Keeps the durations lined up with the rows that have a play button.
                Spacer(Modifier.size(IconSize.minTouchTarget))
            }
        }
    }
}

private enum class PreviewButtonState { Idle, Loading, Playing }

@Composable
private fun PreviewButton(trackTitle: String, playback: TrackPlayback?, onClick: () -> Unit) {
    val description = stringResource(
        if (playback == null) R.string.play_preview else R.string.stop_preview,
        trackTitle
    )
    val state = when {
        playback == null -> PreviewButtonState.Idle
        playback.isLoading -> PreviewButtonState.Loading
        else -> PreviewButtonState.Playing
    }
    val motion = MaterialTheme.motionScheme
    // The description sits on the button rather than its icon, since a buffering clip shows a
    // spinner in the icon's place — and tapping it then stops the clip, same as while playing.
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description }) {
        AnimatedContent(
            targetState = state,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (fadeIn(motion.fastEffectsSpec()) + scaleIn(
                    motion.fastSpatialSpec(),
                    initialScale = 0.6f
                ))
                    .togetherWith(fadeOut(motion.fastEffectsSpec()))
            },
            label = "previewButtonIcon"
        ) { buttonState ->
            when (buttonState) {
                PreviewButtonState.Idle -> Icon(Icons.Filled.PlayArrow, contentDescription = null)
                PreviewButtonState.Loading -> CircularProgressIndicator(
                    strokeWidth = IconSize.previewSpinnerStroke,
                    modifier = Modifier.size(IconSize.previewSpinner)
                )

                PreviewButtonState.Playing -> Icon(Icons.Filled.Pause, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StreamingLinksRow(streamingLinks: StreamingLinks, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        items(streamingLinks.asDisplayList()) { (label, url) ->
            AssistChip(onClick = { uriHandler.openUri(url) }, label = { Text(label) })
        }
    }
}

@Composable
private fun AlbumTypeAndYear(album: AlbumDetail, modifier: Modifier = Modifier) {
    val year = album.firstReleaseDate?.year
    val text = listOfNotNull(album.primaryType, year?.toString()).joinToString(separator = " · ")
    if (text.isNotEmpty()) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumDetailScreenPreview(
    @PreviewParameter(AlbumDetailUiStatePreviewProvider::class) state: AlbumDetailUiState
) {
    CassetteTheme {
        AlbumDetailScreenContent(state = state, onIntent = {}, onBack = {})
    }
}
