package dev.anilbeesetti.nextplayer.feature.more.screens.history

import androidx.lifecycle.viewModelScope
import dev.anilbeesetti.nextplayer.core.data.repository.MediaRepository
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.base.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HistoryViewModel(
    private val mediaRepository: MediaRepository,
    preferencesRepository: PreferencesRepository,
    @InjectedParam internal var output: Output,
) : MviViewModel<HistoryUiState, HistoryAction>() {

    data class Output(
        val navigateUp: () -> Unit,
        val playVideo: (String) -> Unit,
    )

    private val stateInternal = MutableStateFlow(HistoryUiState())
    override val state: StateFlow<HistoryUiState> = stateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.observePlaybackHistory()
                .catch { error ->
                    stateInternal.update { it.copy(history = DataState.Error(error)) }
                }
                .collect { history ->
                    stateInternal.update { currentState ->
                        val validSelected = currentState.selectedUris.intersect(history.map { it.uriString }.toSet())
                        currentState.copy(
                            history = DataState.Success(history),
                            selectedUris = validSelected,
                            isSelectionMode = currentState.isSelectionMode && validSelected.isNotEmpty(),
                        )
                    }
                }
        }
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { preferences ->
                stateInternal.update { it.copy(preferences = preferences) }
            }
        }
    }

    override fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.NavigateUp -> {
                if (state.value.isSelectionMode) {
                    stateInternal.update { it.copy(isSelectionMode = false, selectedUris = emptySet()) }
                } else {
                    output.navigateUp()
                }
            }
            is HistoryAction.PlayVideo -> output.playVideo(action.uri)
            is HistoryAction.ClearHistory -> clearHistory()
            is HistoryAction.ToggleSelection -> toggleSelection(action.uri)
            is HistoryAction.SelectAll -> selectAll()
            is HistoryAction.ClearSelection -> clearSelection()
            is HistoryAction.DeleteSelected -> deleteSelected()
            is HistoryAction.DeleteSingle -> deleteSingle(action.uri)
        }
    }

    private fun toggleSelection(uri: String) {
        stateInternal.update { currentState ->
            val updated = if (currentState.selectedUris.contains(uri)) {
                currentState.selectedUris - uri
            } else {
                currentState.selectedUris + uri
            }
            currentState.copy(
                selectedUris = updated,
                isSelectionMode = updated.isNotEmpty(),
            )
        }
    }

    private fun selectAll() {
        val currentList = (state.value.history as? DataState.Success)?.value.orEmpty()
        val allUris = currentList.map { it.uriString }.toSet()
        stateInternal.update { currentState ->
            if (currentState.selectedUris.size == allUris.size) {
                currentState.copy(selectedUris = emptySet(), isSelectionMode = false)
            } else {
                currentState.copy(selectedUris = allUris, isSelectionMode = allUris.isNotEmpty())
            }
        }
    }

    private fun clearSelection() {
        stateInternal.update { it.copy(selectedUris = emptySet(), isSelectionMode = false) }
    }

    private fun deleteSelected() {
        val urisToDelete = state.value.selectedUris.toList()
        if (urisToDelete.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.removeVideosFromPlaybackHistory(urisToDelete)
            stateInternal.update { it.copy(selectedUris = emptySet(), isSelectionMode = false) }
        }
    }

    private fun deleteSingle(uri: String) {
        viewModelScope.launch {
            mediaRepository.removeVideosFromPlaybackHistory(listOf(uri))
            stateInternal.update { currentState ->
                val updated = currentState.selectedUris - uri
                currentState.copy(
                    selectedUris = updated,
                    isSelectionMode = currentState.isSelectionMode && updated.isNotEmpty(),
                )
            }
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            mediaRepository.clearPlaybackHistory()
            stateInternal.update { it.copy(selectedUris = emptySet(), isSelectionMode = false) }
        }
    }
}

data class HistoryUiState(
    val history: DataState<List<Video>> = DataState.Loading,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
    val selectedUris: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
)

sealed interface HistoryAction {
    data object NavigateUp : HistoryAction
    data class PlayVideo(val uri: String) : HistoryAction
    data object ClearHistory : HistoryAction
    data class ToggleSelection(val uri: String) : HistoryAction
    data object SelectAll : HistoryAction
    data object ClearSelection : HistoryAction
    data object DeleteSelected : HistoryAction
    data class DeleteSingle(val uri: String) : HistoryAction
}
