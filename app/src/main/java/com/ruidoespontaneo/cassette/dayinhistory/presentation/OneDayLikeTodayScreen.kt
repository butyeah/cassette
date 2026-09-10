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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.dayinhistory.domain.model.AlbumsByYear
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OneDayLikeTodayScreen(
    modifier: Modifier = Modifier,
    viewModel: OneDayLikeTodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OneDayLikeTodayScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun OneDayLikeTodayScreenContent(
    state: OneDayLikeTodayUiState,
    onIntent: (OneDayLikeTodayIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DayHeader(
            day = state.day,
            onPrevious = { onIntent(OneDayLikeTodayIntent.PreviousDay) },
            onNext = { onIntent(OneDayLikeTodayIntent.NextDay) },
            onDateClick = { onIntent(OneDayLikeTodayIntent.ToggleCalendar) }
        )
        if (state.isCalendarExpanded) {
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

            else -> AlbumsByYearList(groups = state.albumsByYear, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun DayHeader(
    day: MonthDay,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(
            text = day.format(DAY_FORMAT),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clickable(onClick = onDateClick)
        )
        TextButton(onClick = onNext) { Text("›") }
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
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun AlbumsByYearList(groups: List<AlbumsByYear>, modifier: Modifier = Modifier) {
    if (groups.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No albums released on this day")
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups, key = { it.year }) { group -> YearCard(group) }
    }
}

@Composable
private fun YearCard(group: AlbumsByYear, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = group.year.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            group.albums.forEach { album -> AlbumRow(album) }
        }
    }
}

@Composable
private fun AlbumRow(album: Album) {
    ListItem(
        headlineContent = { Text(album.title) },
        supportingContent = { Text(album.artistName) }
    )
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())

// MonthDay carries no year, but DatePickerState needs a UTC millis timestamp — pick whatever
// nearby year makes this day valid (matters only for Feb 29 landing in a non-leap year).
private fun MonthDay.toUtcMillis(): Long {
    val year = nearestValidYear(this, LocalDate.now().year)
    return atYear(year).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun nearestValidYear(day: MonthDay, from: Int): Int =
    generateSequence(from) { it + 1 }.first { day.isValidYear(it) }

private val previewGroups = listOf(
    AlbumsByYear(
        year = 2001,
        albums = listOf(
            Album(
                id = "1",
                title = "Origin of Symmetry",
                releaseDate = LocalDate.of(2001, 6, 17),
                artistId = null,
                artistName = "Muse"
            )
        )
    ),
    AlbumsByYear(
        year = 1994,
        albums = listOf(
            Album(
                id = "2",
                title = "The Downward Spiral",
                releaseDate = LocalDate.of(1994, 6, 17),
                artistId = null,
                artistName = "Nine Inch Nails"
            ),
            Album(
                id = "3",
                title = "Superunknown",
                releaseDate = LocalDate.of(1994, 6, 17),
                artistId = null,
                artistName = "Soundgarden"
            )
        )
    )
)

@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenListPreview() {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = OneDayLikeTodayUiState(
                day = MonthDay.of(6, 17),
                isLoading = false,
                albumsByYear = previewGroups
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenCalendarPreview() {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = OneDayLikeTodayUiState(
                day = MonthDay.of(6, 17),
                isLoading = false,
                albumsByYear = previewGroups,
                isCalendarExpanded = true
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenEmptyPreview() {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = OneDayLikeTodayUiState(day = MonthDay.of(6, 17), isLoading = false),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenLoadingPreview() {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = OneDayLikeTodayUiState(day = MonthDay.of(6, 17), isLoading = true),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OneDayLikeTodayScreenErrorPreview() {
    CassetteTheme {
        OneDayLikeTodayScreenContent(
            state = OneDayLikeTodayUiState(
                day = MonthDay.of(6, 17),
                isLoading = false,
                errorMessage = "Couldn't load albums"
            ),
            onIntent = {}
        )
    }
}
