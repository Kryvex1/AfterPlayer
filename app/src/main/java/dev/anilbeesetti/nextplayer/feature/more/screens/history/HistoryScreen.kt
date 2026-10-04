package dev.anilbeesetti.nextplayer.feature.more.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.CenterCircularProgressBar
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.VideoListItem

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HistoryScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun HistoryScreenContent(
    state: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isSelectionMode) {
        onAction(HistoryAction.ClearSelection)
    }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                NextTopAppBar(
                    title = stringResource(R.string.items_selected, state.selectedUris.size),
                    navigationIcon = {
                        FilledTonalIconButton(
                            onClick = { onAction(HistoryAction.ClearSelection) },
                            modifier = Modifier.tvFocusRing(),
                        ) {
                            Icon(
                                imageVector = NextIcons.Close,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { onAction(HistoryAction.SelectAll) },
                            modifier = Modifier.tvFocusRing(),
                        ) {
                            Icon(
                                imageVector = NextIcons.SelectAll,
                                contentDescription = stringResource(R.string.select_all),
                            )
                        }
                        IconButton(
                            onClick = { showDeleteSelectedConfirmation = true },
                            enabled = state.selectedUris.isNotEmpty(),
                            modifier = Modifier.tvFocusRing(),
                        ) {
                            Icon(
                                imageVector = NextIcons.Delete,
                                contentDescription = stringResource(R.string.delete_selected),
                            )
                        }
                    },
                )
            } else {
                NextTopAppBar(
                    title = stringResource(R.string.history),
                    navigationIcon = {
                        FilledTonalIconButton(
                            onClick = { onAction(HistoryAction.NavigateUp) },
                            modifier = Modifier.tvFocusRing(),
                        ) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_up),
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = state.history.result.orEmpty().isNotEmpty(),
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.tvFocusRing(),
                        ) {
                            Text(stringResource(R.string.clear_all))
                        }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (val history = state.history) {
                is DataState.Loading -> CenterCircularProgressBar()
                is DataState.Error -> Text(
                    text = history.value.message.orEmpty(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                is DataState.Success -> LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(history.value, key = { _, video -> video.uriString }) { index, video ->
                        val isSelected = state.selectedUris.contains(video.uriString)
                        VideoListItem(
                            video = video,
                            isRecentlyPlayedVideo = false,
                            preferences = state.preferences,
                            isFirstItem = index == 0,
                            isLastItem = index == history.value.lastIndex,
                            selected = isSelected,
                            onClick = {
                                if (state.isSelectionMode) {
                                    onAction(HistoryAction.ToggleSelection(video.uriString))
                                } else {
                                    onAction(HistoryAction.PlayVideo(video.uriString))
                                }
                            },
                            onLongClick = {
                                onAction(HistoryAction.ToggleSelection(video.uriString))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteSelectedConfirmation) {
        NextDialog(
            onDismissRequest = { showDeleteSelectedConfirmation = false },
            title = { Text(stringResource(R.string.delete_selected)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(HistoryAction.DeleteSelected)
                        showDeleteSelectedConfirmation = false
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            content = {
                Text(stringResource(R.string.delete_selected_confirmation, state.selectedUris.size))
            },
        )
    }

    if (showClearConfirmation) {
        NextDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_history)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(HistoryAction.ClearHistory)
                        showClearConfirmation = false
                    },
                ) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            content = { Text(stringResource(R.string.clear_history_confirmation)) },
        )
    }
}
