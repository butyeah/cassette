package com.ruidoespontaneo.cassette.nowplaying.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.ruidoespontaneo.cassette.ui.components.CoverArt
import com.ruidoespontaneo.cassette.R
import com.ruidoespontaneo.cassette.albumdetail.preview.NowPlaying
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.ui.icons.Pause
import com.ruidoespontaneo.cassette.ui.icons.SkipNext
import com.ruidoespontaneo.cassette.ui.icons.SkipPrevious
import com.ruidoespontaneo.cassette.ui.theme.IconSize

/**
 * The album cover, the track and album, and the controls: one big button — ❚❚ stops the preview,
 * ▶ plays the track again from the start (autoplay carries on from there), a spinner while it
 * buffers or an album is being looked up — between ⏮ and ⏭, which move through the day's previews
 * in autoplay order, across albums. Follows autoplay live while it's open, [lyrics] included; the
 * whole dialog scrolls, so long lyrics never push the controls out of reach.
 */
@Composable
fun NowPlayingDialog(
    nowPlaying: NowPlaying,
    status: NowPlayingStatus,
    lyrics: LyricsUiState,
    onIntent: (NowPlayingIntent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CoverArt(
                    url = nowPlaying.album.coverArtUrl(),
                    contentDescription = null, // decorative — the titles below name the album
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Spacing.medium))
                )
                Text(
                    text = nowPlaying.trackTitle.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.large)
                )
                Text(
                    text = stringResource(R.string.now_playing_album_artist, nowPlaying.album.title, nowPlaying.album.artistName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.extraSmall)
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onIntent(NowPlayingIntent.Previous) }, enabled = nowPlaying.hasPrevious) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.now_playing_previous))
                    }
                    PlayStopButton(status = status, onIntent = onIntent)
                    IconButton(onClick = { onIntent(NowPlayingIntent.Next) }, enabled = nowPlaying.hasNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.now_playing_next))
                    }
                }
                LyricsSection(lyrics = lyrics, modifier = Modifier.padding(top = Spacing.extraLarge))
            }
        }
    }
}

@Composable
private fun PlayStopButton(status: NowPlayingStatus, onIntent: (NowPlayingIntent) -> Unit, modifier: Modifier = Modifier) {
    val isStopped = status == NowPlayingStatus.Stopped
    val description = stringResource(if (isStopped) R.string.now_playing_replay else R.string.now_playing_stop)
    FilledIconButton(
        onClick = { onIntent(if (isStopped) NowPlayingIntent.Replay else NowPlayingIntent.Stop) },
        modifier = modifier
            .size(IconSize.nowPlayingButton)
            .semantics { contentDescription = description }
    ) {
        when (status) {
            NowPlayingStatus.Stopped -> Icon(Icons.Filled.PlayArrow, contentDescription = null)
            NowPlayingStatus.Loading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = IconSize.previewSpinnerStroke,
                modifier = Modifier.size(IconSize.previewSpinner)
            )

            NowPlayingStatus.Playing -> Icon(Icons.Filled.Pause, contentDescription = null)
        }
    }
}

/** The playing track's lyrics under a heading, or why there are none, then LRCLIB's credit. */
@Composable
private fun LyricsSection(lyrics: LyricsUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.lyrics_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        when (lyrics) {
            LyricsUiState.Loading -> CircularProgressIndicator(
                strokeWidth = IconSize.previewSpinnerStroke,
                modifier = Modifier
                    .padding(top = Spacing.medium)
                    .size(IconSize.previewSpinner)
            )

            is LyricsUiState.Found -> SelectionContainer {
                Text(
                    text = lyrics.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }

            LyricsUiState.Instrumental -> LyricsMessage(R.string.lyrics_instrumental)
            LyricsUiState.NotFound -> LyricsMessage(R.string.lyrics_not_found)
            LyricsUiState.Failed -> LyricsMessage(R.string.lyrics_failed)
        }
        Text(
            text = stringResource(R.string.lyrics_credit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.large)
        )
    }
}

@Composable
private fun LyricsMessage(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.small)
    )
}
