package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.ruidoespontaneo.cassette.ui.icons.GridView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.ui.components.CoverArt
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.components.AnimatedGradientBackground
import com.ruidoespontaneo.cassette.cover.components.CoverCard
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.ui.theme.ToolbarSize
import com.ruidoespontaneo.cassette.dayinhistory.domain.layout.mosaicCells
import com.ruidoespontaneo.cassette.dayinhistory.domain.layout.mosaicRowCount
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import com.ruidoespontaneo.cassette.ui.theme.IconSize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.Instant
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun OneDayLikeTodayScreen(
    onAlbumClick: (MonthDay, String) -> Unit,
    isCalendarOpen: Boolean,
    onCalendarDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OneDayLikeTodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OneDayLikeTodayScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAlbumClick = onAlbumClick,
        isCalendarOpen = isCalendarOpen,
        onCalendarDismiss = onCalendarDismiss,
        dayFormatter = rememberDayFormatter(),
        modifier = modifier
    )
}

@Composable
private fun OneDayLikeTodayScreenContent(
    state: OneDayLikeTodayUiState,
    onIntent: (OneDayLikeTodayIntent) -> Unit,
    onAlbumClick: (MonthDay, String) -> Unit,
    isCalendarOpen: Boolean,
    onCalendarDismiss: () -> Unit,
    dayFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val hazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedGradientBackground(Modifier.matchParentSize().hazeSource(hazeState))
        Column(modifier = Modifier.fillMaxSize()) {
            DayHeader(
                day = state.day,
                layout = state.layout,
                onToggleLayout = { onIntent(OneDayLikeTodayIntent.ToggleLayout) },
                dayFormatter = dayFormatter
            )
            when {
                state.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
                state.errorRes != null -> ErrorMessage(
                    message = stringResource(state.errorRes),
                    onRetry = { onIntent(OneDayLikeTodayIntent.Retry) },
                    modifier = Modifier.fillMaxSize()
                )

                state.albumsByYear.isEmpty() -> NoAlbums(Modifier.fillMaxSize())
                state.layout == AlbumsLayout.Grid -> AlbumsGrid(
                    day = state.day,
                    groups = state.albumsByYear,
                    onAlbumClick = { albumId -> onAlbumClick(state.day, albumId) },
                    modifier = Modifier.fillMaxSize()
                )

                else -> AlbumsByYearList(
                    groups = state.albumsByYear,
                    onAlbumClick = { albumId -> onAlbumClick(state.day, albumId) },
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    if (isCalendarOpen) {
        DayCalendarDialog(
            day = state.day,
            onDaySelected = {
                onIntent(OneDayLikeTodayIntent.SelectDate(it))
                onCalendarDismiss()
            },
            onDismiss = onCalendarDismiss
        )
    }
}

@Composable
private fun DayHeader(
    day: MonthDay,
    layout: AlbumsLayout,
    onToggleLayout: () -> Unit,
    dayFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // As wide as the layout toggle, balancing it so the date stays centered.
        Spacer(Modifier.size(IconSize.minTouchTarget))
        Text(
            text = day.format(dayFormatter),
            style = MaterialTheme.typography.titleLarge
        )
        LayoutToggle(layout = layout, onClick = onToggleLayout)
    }
}

/** Shows the layout it switches *to*: the grid icon while listing, the list icon while in the grid. */
@Composable
private fun LayoutToggle(layout: AlbumsLayout, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        when (layout) {
            AlbumsLayout.List -> Icon(Icons.Filled.GridView, contentDescription = stringResource(R.string.show_as_grid))
            AlbumsLayout.Grid -> Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.show_as_list)
            )
        }
    }
}

/**
 * A Material3 [DatePicker] dialog used to jump straight to a day — year-agnostic domain, so only
 * the tapped month/day matter; whatever year the picker happens to show is otherwise irrelevant.
 * Tapping a day picks it right away, so there's no confirm button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCalendarDialog(day: MonthDay, onDaySelected: (MonthDay) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = day.toUtcMillis())
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: return@LaunchedEffect
        val selected = MonthDay.from(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        if (selected != day) onDaySelected(selected)
    }
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {}) {
        DatePicker(state = datePickerState, showModeToggle = false, headline = null)
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        Spacer(Modifier.height(Spacing.small))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun NoAlbums(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.no_albums_message))
    }
}

// Shared by the list and the grid; the bottom clears the floating toolbar.
private val albumsContentPadding = PaddingValues(
    start = Spacing.large,
    top = Spacing.large,
    end = Spacing.large,
    bottom = Spacing.large + ToolbarSize.clearance
)

@Composable
private fun AlbumsByYearList(
    groups: List<AlbumsByYear>,
    onAlbumClick: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = albumsContentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        items(groups, key = { it.year }) { group ->
            YearCard(group, onAlbumClick = onAlbumClick, hazeState = hazeState)
        }
    }
}

@Composable
private fun YearCard(
    group: AlbumsByYear,
    onAlbumClick: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    // AlbumRow's ListItem needs a transparent containerColor too, per CoverCard's own doc — it
    // otherwise paints its own background over the blur.
    CoverCard(hazeState = hazeState, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = Spacing.small)) {
            Text(
                text = group.year.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.extraSmall)
            )
            group.albums.forEach { album -> AlbumRow(album, onClick = { onAlbumClick(album.id) }) }
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            CoverArt(
                url = album.coverArtUrl(),
                contentDescription = null, // decorative — title/artist are already read by the row
                modifier = Modifier
                    .size(IconSize.albumArt)
                    .clip(RoundedCornerShape(Spacing.extraSmall))
            )
        },
        headlineContent = { Text(album.title) },
        supportingContent = { Text(album.artistName) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Covers only, [GRID_COLUMNS] to a row, split by release year: each year's label, then its covers
 * as a [CoverMosaic]. Same order as the list, which is also the pager's order.
 *
 * Tapping a cover expands it in place, with its title and artist; tapping the expanded cover opens
 * the album. Which cover is expanded is plain UI state, reset whenever the [day] changes.
 */
@Composable
private fun AlbumsGrid(
    day: MonthDay,
    groups: List<AlbumsByYear>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedAlbumId by rememberSaveable(day) { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = modifier,
        contentPadding = albumsContentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        groups.forEachIndexed { index, group ->
            item(key = "year-${group.year}") {
                Text(
                    text = group.year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        // Extra room above every year but the first, so each reads as its own group.
                        .padding(top = if (index == 0) 0.dp else Spacing.medium)
                        .semantics { heading() }
                )
            }
            item(key = "covers-${group.year}") {
                CoverMosaic(
                    albums = group.albums,
                    expandedAlbumId = expandedAlbumId,
                    onCoverClick = { albumId ->
                        if (albumId == expandedAlbumId) onAlbumClick(albumId) else expandedAlbumId = albumId
                    }
                )
            }
        }
    }
}

/**
 * One year's covers, [GRID_COLUMNS] square tiles to a row, packed by [mosaicCells]: the expanded
 * cover takes 2×2 tiles and the rest flow around it. Changing which cover is expanded animates
 * every cover to its new bounds on the expressive spatial spring.
 *
 * The mosaic's height animates on the same spring, clipping to it as it goes, and the list items
 * below just sit under it: animating them separately let them lag behind the growing covers,
 * which then drew over the next year until everything settled.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CoverMosaic(
    albums: List<Album>,
    expandedAlbumId: String?,
    onCoverClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedIndex = albums.indexOfFirst { it.id == expandedAlbumId }.takeIf { it >= 0 }
    val cells = remember(albums.size, expandedIndex) { mosaicCells(albums.size, expandedIndex, GRID_COLUMNS) }
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val boundsTransform = remember(spatialSpec) { BoundsTransform { _, _ -> spatialSpec } }
    // animateContentSize clips to the animated size, so covers moving past the bottom edge are
    // cropped for a moment rather than drawn over the next year. It sits outside the
    // LookaheadScope, as a plain size animation of the whole mosaic.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
    ) {
        LookaheadScope {
            Layout(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    albums.forEach { album ->
                        key(album.id) {
                            CoverTile(
                                album = album,
                                expanded = album.id == expandedAlbumId,
                                onClick = { onCoverClick(album.id) },
                                modifier = Modifier.animateBounds(this, boundsTransform = boundsTransform)
                            )
                        }
                    }
                }
            ) { measurables, constraints ->
                val gap = Spacing.small.roundToPx()
                val tile = (constraints.maxWidth - gap * (GRID_COLUMNS - 1)) / GRID_COLUMNS
                val placeables = measurables.mapIndexed { index, measurable ->
                    val side = tile * cells[index].span + gap * (cells[index].span - 1)
                    measurable.measure(Constraints.fixed(side, side))
                }
                val rows = mosaicRowCount(cells)
                val height = if (rows == 0) 0 else rows * tile + (rows - 1) * gap
                layout(constraints.maxWidth, height) {
                    placeables.forEachIndexed { index, placeable ->
                        placeable.place(cells[index].column * (tile + gap), cells[index].row * (tile + gap))
                    }
                }
            }
        }
    }
}

/** A cover; while [expanded], its title and artist fade in over a scrim along the bottom. */
@Composable
private fun CoverTile(album: Album, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Spacing.small))
            .clickable(
                onClickLabel = stringResource(if (expanded) R.string.open_album else R.string.show_album_details),
                onClick = onClick
            )
    ) {
        CoverArt(
            url = album.coverArtUrl(),
            // The cover is all there is to go on while collapsed, so it names the album.
            contentDescription = stringResource(R.string.album_cover_description, album.title, album.artistName),
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(effects),
            exit = fadeOut(effects),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = SCRIM_ALPHA))))
                    .padding(Spacing.small)
                    // The cover's description already names the album and artist.
                    .clearAndSetSemantics {}
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.7f

private const val GRID_COLUMNS = 4


@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenPreview(
    @PreviewParameter(OneDayLikeTodayUiStatePreviewProvider::class) state: OneDayLikeTodayUiState
) {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = state,
            onIntent = {},
            onAlbumClick = { _, _ -> },
            isCalendarOpen = false,
            onCalendarDismiss = {},
            dayFormatter = rememberDayFormatter()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenCalendarPreview() {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = OneDayLikeTodayUiState(day = MonthDay.of(6, 17), isLoading = false),
            onIntent = {},
            onAlbumClick = { _, _ -> },
            isCalendarOpen = true,
            onCalendarDismiss = {},
            dayFormatter = rememberDayFormatter()
        )
    }
}
