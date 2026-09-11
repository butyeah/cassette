package com.ruidoespontaneo.cassette.albumdetail.presentation

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.toBitmap
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.components.AnimatedGradientBackground
import com.ruidoespontaneo.cassette.cover.components.CoverCard
import com.ruidoespontaneo.cassette.cover.components.dominantColors
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.musicbrainz.presentation.durationText
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
    val titleAlpha by animateFloatAsState(targetValue = if (showTitle) 1f else 0f, label = "titleAlpha")

    // The background takes on the cover art's own dominant colors once it's decoded — falls back
    // to AnimatedGradientBackground's theme-colored default (null) until then, or if extraction
    // comes up short.
    var coverBitmap by remember(state.album?.id) { mutableStateOf<Bitmap?>(null) }
    var dominantColors by remember(state.album?.id) { mutableStateOf<List<Color>?>(null) }
    LaunchedEffect(coverBitmap) { dominantColors = coverBitmap?.dominantColors() }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedGradientBackground(Modifier.matchParentSize().hazeSource(hazeState), colors = dominantColors)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            when {
                state.isLoading -> LoadingIndicator(Modifier.fillMaxSize().padding(innerPadding))
                state.errorMessage != null -> ErrorMessage(
                    message = state.errorMessage,
                    onRetry = { onIntent(AlbumDetailIntent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )

                state.album != null -> AlbumDetailContent(
                    album = state.album,
                    hazeState = hazeState,
                    scrollState = scrollState,
                    onCoverLoaded = { coverBitmap = it },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
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
    hazeState: HazeState,
    scrollState: ScrollState,
    onCoverLoaded: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    CoverCard(hazeState = hazeState, modifier = modifier) {
        Column(modifier = Modifier.verticalScroll(scrollState).padding(Spacing.large)) {
            val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            AsyncImage(
                model = album.coverArtUrl(),
                contentDescription = null, // decorative — title/artist are already read by the screen
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                onSuccess = { onCoverLoaded(it.result.image.toBitmap()) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(IconSize.albumArtLarge)
                    .clip(RoundedCornerShape(Spacing.small))
            )
            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Spacing.medium)
            )
            Text(text = album.artistName, style = MaterialTheme.typography.titleMedium)
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
                    text = stringResource(R.string.rating_format, ratingValue, album.ratingVotesCount),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }
            if (album.tracks.isNotEmpty()) {
                Tracklist(album.tracks, modifier = Modifier.padding(top = Spacing.large))
            }
        }
    }
}

@Composable
private fun Tracklist(tracks: List<Track>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.tracklist_title), style = MaterialTheme.typography.titleMedium)
        tracks.forEach { track -> TrackRow(track, modifier = Modifier.padding(top = Spacing.small)) }
    }
}

@Composable
private fun TrackRow(track: Track, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "${track.position}. ${track.title}",
            style = MaterialTheme.typography.bodyMedium,
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
