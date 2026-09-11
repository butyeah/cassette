package com.ruidoespontaneo.cassette.albumpager.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.albumdetail.presentation.AlbumDetailScreen
import com.ruidoespontaneo.cassette.albumdetail.presentation.AlbumDetailViewModel
import com.ruidoespontaneo.cassette.ui.theme.Spacing

/**
 * Swipes between every album released on one day (across years) — see [AlbumPagerViewModel].
 * Each page is a full [AlbumDetailScreen] (its own top bar, back button, tracklist, ...), so the
 * whole screen swipes together rather than just the body under a fixed chrome.
 */
@Composable
fun AlbumPagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumPagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlbumPagerScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun AlbumPagerScreenContent(
    state: AlbumPagerUiState,
    onIntent: (AlbumPagerIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> LoadingScaffold(onBack, modifier)
        state.errorMessage != null -> ErrorScaffold(
            message = state.errorMessage,
            onRetry = { onIntent(AlbumPagerIntent.Retry) },
            onBack = onBack,
            modifier = modifier
        )

        else -> AlbumPager(
            albumIds = state.albumIds,
            initialPage = state.initialPage,
            onBack = onBack,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AlbumPager(
    albumIds: List<String>,
    initialPage: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { albumIds.size }
    HorizontalPager(state = pagerState, modifier = modifier, key = { albumIds[it] }) { page ->
        val albumId = albumIds[page]
        AlbumDetailScreen(
            onBack = onBack,
            viewModel = hiltViewModel<AlbumDetailViewModel, AlbumDetailViewModel.Factory>(key = albumId) { factory ->
                factory.create(albumId)
            }
        )
    }
}

// Loading/error states get the same top-bar-with-back-button chrome AlbumDetailScreen uses for
// its own loading/error states — once albumIds is populated, each page brings its own instead.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScaffold(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AlbumPagerTopBar(onBack) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorScaffold(message: String, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AlbumPagerTopBar(onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPagerTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.album_detail_default_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
    )
}
