package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruidoespontaneo.cassette.BuildConfig
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.cover.components.AnimatedGradientBackground
import com.ruidoespontaneo.cassette.cover.components.CoverCard
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.ui.theme.ToolbarSize
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
import java.util.Locale

@Composable
fun OneDayLikeTodayScreen(
    onAlbumClick: (MonthDay, String) -> Unit,
    isCalendarOpen: Boolean,
    onCalendarDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dayFormatter: DateTimeFormatter,
    viewModel: OneDayLikeTodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OneDayLikeTodayScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAlbumClick = onAlbumClick,
        isCalendarOpen = isCalendarOpen,
        onCalendarDismiss = onCalendarDismiss,
        dayFormatter = dayFormatter,
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
                onPrevious = { onIntent(OneDayLikeTodayIntent.PreviousDay) },
                onNext = { onIntent(OneDayLikeTodayIntent.NextDay) },
                onToggleLayout = { onIntent(OneDayLikeTodayIntent.ToggleLayout) },
                dayFormatter = dayFormatter
            )
            when {
                state.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
                state.errorMessage != null -> ErrorMessage(
                    message = state.errorMessage,
                    onRetry = { onIntent(OneDayLikeTodayIntent.Retry) },
                    modifier = Modifier.fillMaxSize()
                )

                state.albumsByYear.isEmpty() -> NoAlbums(Modifier.fillMaxSize())
                state.layout == AlbumsLayout.Grid -> AlbumsGrid(
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
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
        // Previous/next are still being tested — only expose them in dev builds. Without them, a
        // spacer as wide as the layout toggle balances it so the date stays centered.
        if (BuildConfig.DEBUG) {
            TextButton(onClick = onPrevious) { Text(stringResource(R.string.previous_day)) }
        } else {
            Spacer(Modifier.size(IconSize.minTouchTarget))
        }
        Text(
            text = day.format(dayFormatter),
            style = MaterialTheme.typography.titleLarge
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (BuildConfig.DEBUG) {
                TextButton(onClick = onNext) { Text(stringResource(R.string.next_day)) }
            }
            LayoutToggle(layout = layout, onClick = onToggleLayout)
        }
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
            val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            AsyncImage(
                model = album.coverArtUrl(),
                contentDescription = null, // decorative — title/artist are already read by the row
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
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
 * Covers only, [GRID_COLUMNS] to a row, split by release year: each year's label spans the full
 * width, then its covers follow. Same order as the list, which is also the pager's order.
 */
@Composable
private fun AlbumsGrid(groups: List<AlbumsByYear>, onAlbumClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = modifier,
        contentPadding = albumsContentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        groups.forEachIndexed { index, group ->
            item(key = "year-${group.year}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = group.year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        // Extra room above every year but the first, so each reads as its own group.
                        .padding(top = if (index == 0) 0.dp else Spacing.medium)
                        .semantics { heading() }
                )
            }
            gridItems(group.albums, key = { it.id }) { album ->
                AsyncImage(
                    model = album.coverArtUrl(),
                    // The cover is all there is to go on here, so it names the album.
                    contentDescription = stringResource(R.string.album_cover_description, album.title, album.artistName),
                    placeholder = placeholder,
                    error = placeholder,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Spacing.small))
                        .clickable { onAlbumClick(album.id) }
                )
            }
        }
    }
}

private const val GRID_COLUMNS = 4

private val previewDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())

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
            dayFormatter = previewDayFormatter
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
            dayFormatter = previewDayFormatter
        )
    }
}
