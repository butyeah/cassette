package com.ruidoespontaneo.cassette.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun CalendarScreenContent(
    state: CalendarUiState,
    onIntent: (CalendarIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MonthHeader(
            month = state.month,
            onPrevious = { onIntent(CalendarIntent.PreviousMonth) },
            onNext = { onIntent(CalendarIntent.NextMonth) }
        )
        when {
            state.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
            state.errorMessage != null -> ErrorMessage(
                message = state.errorMessage,
                onRetry = { onIntent(CalendarIntent.Retry) },
                modifier = Modifier.fillMaxSize()
            )

            else -> AlbumList(albums = state.albums, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
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
            text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onNext) { Text("›") }
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
private fun AlbumList(albums: List<Album>, modifier: Modifier = Modifier) {
    if (albums.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No albums released this month")
        }
        return
    }
    LazyColumn(modifier = modifier) {
        items(albums, key = { it.id }) { album -> AlbumRow(album) }
    }
}

@Composable
private fun AlbumRow(album: Album) {
    ListItem(
        headlineContent = { Text(album.title) },
        supportingContent = { Text(album.artistName) },
        trailingContent = {
            album.releaseDate?.let { Text(it.format(DateTimeFormatter.ofPattern("MMM d"))) }
        }
    )
}

private val previewAlbums = listOf(
    Album(
        id = "1",
        title = "Origin of Symmetry",
        releaseDate = LocalDate.of(2001, 2, 19),
        artistId = "muse",
        artistName = "Muse"
    ),
    Album(
        id = "2",
        title = "The Downward Spiral",
        releaseDate = LocalDate.of(1994, 3, 8),
        artistId = "nin",
        artistName = "Nine Inch Nails"
    )
)

@Preview(showBackground = true)
@Composable
private fun CalendarScreenListPreview() {
    CassetteTheme {
        CalendarScreenContent(
            state = CalendarUiState(
                month = YearMonth.of(2024, 2),
                isLoading = false,
                albums = previewAlbums
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenEmptyPreview() {
    CassetteTheme {
        CalendarScreenContent(
            state = CalendarUiState(month = YearMonth.of(2024, 2), isLoading = false),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenLoadingPreview() {
    CassetteTheme {
        CalendarScreenContent(
            state = CalendarUiState(month = YearMonth.of(2024, 2), isLoading = true),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenErrorPreview() {
    CassetteTheme {
        CalendarScreenContent(
            state = CalendarUiState(
                month = YearMonth.of(2024, 2),
                isLoading = false,
                errorMessage = "Couldn't load albums"
            ),
            onIntent = {}
        )
    }
}
