package com.ruidoespontaneo.cassette.dayinhistory.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruidoespontaneo.cassette.BuildConfig
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.ui.theme.AnimatedGradientBackground
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import com.ruidoespontaneo.cassette.ui.theme.IconSize
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
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
    modifier: Modifier = Modifier,
    dayFormatter: DateTimeFormatter,
    viewModel: OneDayLikeTodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OneDayLikeTodayScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAlbumClick = onAlbumClick,
        dayFormatter = dayFormatter,
        modifier = modifier
    )
}

@Composable
private fun OneDayLikeTodayScreenContent(
    state: OneDayLikeTodayUiState,
    onIntent: (OneDayLikeTodayIntent) -> Unit,
    onAlbumClick: (MonthDay, String) -> Unit,
    dayFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val hazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedGradientBackground(Modifier.matchParentSize().hazeSource(hazeState))
        Column(modifier = Modifier.fillMaxSize()) {
            DayHeader(
                day = state.day,
                onPrevious = { onIntent(OneDayLikeTodayIntent.PreviousDay) },
                onNext = { onIntent(OneDayLikeTodayIntent.NextDay) },
                onDateClick = { onIntent(OneDayLikeTodayIntent.ToggleCalendar) },
                dayFormatter = dayFormatter
            )
            if (BuildConfig.DEBUG && state.isCalendarExpanded) {
                DayCalendar(
                    day = state.day,
                    onDaySelected = { onIntent(OneDayLikeTodayIntent.SelectDate(it)) }
                )
            }
            when {
                state.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
                state.errorMessage != null -> ErrorMessage(
                    message = state.errorMessage,
                    onRetry = { onIntent(OneDayLikeTodayIntent.Retry) },
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
}

@Composable
private fun DayHeader(
    day: MonthDay,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
    dayFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        // The day selector (previous/next, jump-to-date calendar) is still being tested — only
        // expose it in dev builds. Center the date on its own once there's nothing to space it
        // between.
        horizontalArrangement = if (BuildConfig.DEBUG) Arrangement.SpaceBetween else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (BuildConfig.DEBUG) {
            TextButton(onClick = onPrevious) { Text(stringResource(R.string.previous_day)) }
        }
        Text(
            text = day.format(dayFormatter),
            style = MaterialTheme.typography.titleLarge,
            modifier = if (BuildConfig.DEBUG) Modifier.clickable(onClick = onDateClick) else Modifier
        )
        if (BuildConfig.DEBUG) {
            TextButton(onClick = onNext) { Text(stringResource(R.string.next_day)) }
        }
    }
}

/**
 * A Material3 [DatePicker] used to jump straight to a day — year-agnostic domain, so only the
 * tapped month/day matter; whatever year the picker happens to show is otherwise irrelevant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCalendar(day: MonthDay, onDaySelected: (MonthDay) -> Unit, modifier: Modifier = Modifier) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = day.toUtcMillis())
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: return@LaunchedEffect
        val selected = MonthDay.from(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        if (selected != day) onDaySelected(selected)
    }
    DatePicker(state = datePickerState, modifier = modifier, showModeToggle = false)
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
private fun AlbumsByYearList(
    groups: List<AlbumsByYear>,
    onAlbumClick: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    if (groups.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_albums_message))
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.large),
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
    // A frosted-glass "crystal" card: transparent container so YearCard's own background doesn't
    // paint over the blur, tinted by hazeEffect's HazeStyle instead — the blur samples whatever's
    // marked with Modifier.hazeSource() behind it (AnimatedGradientBackground). AlbumRow's ListItem
    // needs the same transparent treatment — it paints its own background over this Card's.
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = CardTintAlpha)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(hazeState) {
                style = HazeStyle(tint = HazeTint(surfaceTint), blurRadius = CardBlurRadius)
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
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

private val CardBlurRadius = 20.dp
private const val CardTintAlpha = 0.5f

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
            dayFormatter = previewDayFormatter
        )
    }
}
