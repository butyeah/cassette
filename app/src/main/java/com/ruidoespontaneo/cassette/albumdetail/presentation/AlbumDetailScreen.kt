package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.ui.theme.CassetteTheme
import com.ruidoespontaneo.cassette.ui.theme.IconSize
import com.ruidoespontaneo.cassette.ui.theme.Spacing

@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = hiltViewModel()
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
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.album?.title ?: stringResource(R.string.album_detail_default_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
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
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
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
private fun AlbumDetailContent(album: AlbumDetail, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Spacing.large)) {
        val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
        AsyncImage(
            model = album.coverArtUrl(),
            contentDescription = null, // decorative — title/artist are already read by the screen
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
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
