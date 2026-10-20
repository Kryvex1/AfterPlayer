package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.SubtitleAnimation
import dev.anilbeesetti.nextplayer.core.ui.base.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SubtitlePreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    @InjectedParam internal var output: Output,
) : MviViewModel<SubtitlePreferencesUiState, SubtitlePreferencesUiEvent>() {

    data class Output(
        val navigateUp: () -> Unit,
    )

    private val stateInternal = MutableStateFlow(
        SubtitlePreferencesUiState(
            preferences = preferencesRepository.playerPreferences.value,
        ),
    )
    override val state: StateFlow<SubtitlePreferencesUiState> = stateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { preferences ->
                stateInternal.update { currentState ->
                    currentState.copy(preferences = preferences)
                }
            }
        }
    }

    override fun onAction(action: SubtitlePreferencesUiEvent) {
        when (action) {
            is SubtitlePreferencesUiEvent.NavigateUp -> output.navigateUp()

            is SubtitlePreferencesUiEvent.ShowDialog -> showDialog(action.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleLanguage -> updateSubtitleLanguage(action.value)
            is SubtitlePreferencesUiEvent.UpdateSubtitleFont -> updateSubtitleFont(action.value)
            is SubtitlePreferencesUiEvent.ToggleSubtitleTextBold -> toggleSubtitleTextBold()
            is SubtitlePreferencesUiEvent.UpdateSubtitleFontSize -> updateSubtitleFontSize(action.value)
            is SubtitlePreferencesUiEvent.ToggleSubtitleBackground -> toggleSubtitleBackground()
            is SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles -> toggleApplyEmbeddedStyles()
            is SubtitlePreferencesUiEvent.UpdateSubtitleEncoding -> updateSubtitleEncoding(action.value)
            is SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle -> toggleUseSystemCaptionStyle()
            is SubtitlePreferencesUiEvent.UpdateCustomRegularFont -> updateCustomRegularFont(action.uri)
            is SubtitlePreferencesUiEvent.UpdateCustomBoldFont -> updateCustomBoldFont(action.uri)
            is SubtitlePreferencesUiEvent.UpdateCustomHindiFont -> updateCustomHindiFont(action.uri)
            is SubtitlePreferencesUiEvent.UpdateSubtitleAnimation -> updateSubtitleAnimation(action.animation)
            is SubtitlePreferencesUiEvent.UpdateSubtitleBottomPadding -> updateSubtitleBottomPadding(action.padding)
        }
    }

    private fun showDialog(value: SubtitlePreferenceDialog?) {
        stateInternal.update {
            it.copy(showDialog = value)
        }
    }

    private fun updateSubtitleLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(preferredSubtitleLanguage = value)
            }
        }
    }

    private fun updateSubtitleFont(value: Font) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleFont = value)
            }
        }
    }

    private fun toggleSubtitleTextBold() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleTextBold = !it.subtitleTextBold)
            }
        }
    }

    private fun updateSubtitleFontSize(value: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleTextSize = value)
            }
        }
    }

    private fun toggleSubtitleBackground() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(subtitleBackground = !it.subtitleBackground)
            }
        }
    }

    private fun toggleApplyEmbeddedStyles() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(applyEmbeddedStyles = !it.applyEmbeddedStyles)
            }
        }
    }

    private fun updateSubtitleEncoding(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleTextEncoding = value) }
        }
    }

    private fun toggleUseSystemCaptionStyle() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(useSystemCaptionStyle = !it.useSystemCaptionStyle) }
        }
    }

    private fun updateCustomRegularFont(uri: String?) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(customRegularFontUri = uri) }
        }
    }

    private fun updateCustomBoldFont(uri: String?) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(customBoldFontUri = uri) }
        }
    }

    private fun updateCustomHindiFont(uri: String?) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(customHindiFontUri = uri) }
        }
    }

    private fun updateSubtitleAnimation(animation: SubtitleAnimation) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleAnimation = animation) }
        }
    }

    private fun updateSubtitleBottomPadding(padding: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(subtitleBottomPadding = padding) }
        }
    }
}

@Stable
data class SubtitlePreferencesUiState(
    val showDialog: SubtitlePreferenceDialog? = null,
    val preferences: PlayerPreferences = PlayerPreferences(),
)

sealed interface SubtitlePreferenceDialog {
    data object SubtitleLanguageDialog : SubtitlePreferenceDialog
    data object SubtitleFontDialog : SubtitlePreferenceDialog
    data object SubtitleEncodingDialog : SubtitlePreferenceDialog
    data object SubtitleAnimationDialog : SubtitlePreferenceDialog
}

sealed interface SubtitlePreferencesUiEvent {
    data object NavigateUp : SubtitlePreferencesUiEvent

    data class ShowDialog(val value: SubtitlePreferenceDialog?) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleLanguage(val value: String) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleFont(val value: Font) : SubtitlePreferencesUiEvent
    data object ToggleSubtitleTextBold : SubtitlePreferencesUiEvent
    data class UpdateSubtitleFontSize(val value: Int) : SubtitlePreferencesUiEvent
    data object ToggleSubtitleBackground : SubtitlePreferencesUiEvent
    data object ToggleApplyEmbeddedStyles : SubtitlePreferencesUiEvent
    data class UpdateSubtitleEncoding(val value: String) : SubtitlePreferencesUiEvent
    data object ToggleUseSystemCaptionStyle : SubtitlePreferencesUiEvent
    data class UpdateCustomRegularFont(val uri: String?) : SubtitlePreferencesUiEvent
    data class UpdateCustomBoldFont(val uri: String?) : SubtitlePreferencesUiEvent
    data class UpdateCustomHindiFont(val uri: String?) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleAnimation(val animation: SubtitleAnimation) : SubtitlePreferencesUiEvent
    data class UpdateSubtitleBottomPadding(val padding: Int) : SubtitlePreferencesUiEvent
}
