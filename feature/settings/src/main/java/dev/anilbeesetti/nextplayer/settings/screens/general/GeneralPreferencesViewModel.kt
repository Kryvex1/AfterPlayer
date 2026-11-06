package dev.anilbeesetti.nextplayer.settings.screens.general

import android.util.Xml
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.media.extensions.clearAllCache
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.ControlButtonsPosition
import dev.anilbeesetti.nextplayer.core.model.DoubleTapGesture
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.LoopMode
import dev.anilbeesetti.nextplayer.core.model.MediaLayoutMode
import dev.anilbeesetti.nextplayer.core.model.MediaViewMode
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.Resume
import dev.anilbeesetti.nextplayer.core.model.ScreenOrientation
import dev.anilbeesetti.nextplayer.core.model.Sort
import dev.anilbeesetti.nextplayer.core.model.SubtitleAnimation
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.base.MviViewModel
import java.io.StringReader
import java.io.StringWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.xmlpull.v1.XmlPullParser

@KoinViewModel
class GeneralPreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val imageLoader: ImageLoader,
    @InjectedParam internal var output: Output,
) : MviViewModel<GeneralPreferencesUiState, GeneralPreferencesUiEvent>() {

    data class Output(
        val navigateUp: () -> Unit,
    )

    private val stateInternal = MutableStateFlow(
        GeneralPreferencesUiState(
            playerPreferences = preferencesRepository.playerPreferences.value,
            applicationPreferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    override val state: StateFlow<GeneralPreferencesUiState> = stateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                stateInternal.update { it.copy(playerPreferences = prefs) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { prefs ->
                stateInternal.update { it.copy(applicationPreferences = prefs) }
            }
        }
    }

    override fun onAction(action: GeneralPreferencesUiEvent) {
        when (action) {
            is GeneralPreferencesUiEvent.NavigateUp -> output.navigateUp()

            is GeneralPreferencesUiEvent.ShowDialog -> showDialog(action.value)
            is GeneralPreferencesUiEvent.ClearThumbnailCache -> clearThumbnailCache()
            is GeneralPreferencesUiEvent.ResetSettings -> resetSettings()
            is GeneralPreferencesUiEvent.UpdateMpvConfigStorageUri -> updateMpvConfigStorageUri(action.uri)
            is GeneralPreferencesUiEvent.UpdateMpvConf -> updateMpvConf(action.content)
            is GeneralPreferencesUiEvent.UpdateInputConf -> updateInputConf(action.content)
            is GeneralPreferencesUiEvent.ImportSettingsXml -> importSettingsXml(action.xmlContent, action.onResult)
        }
    }

    private fun showDialog(value: GeneralPreferencesDialog?) {
        stateInternal.update { it.copy(showDialog = value) }
    }

    private fun clearThumbnailCache() {
        viewModelScope.launch {
            imageLoader.clearAllCache()
        }
    }

    private fun resetSettings() {
        viewModelScope.launch {
            preferencesRepository.resetPreferences()
        }
    }

    private fun updateMpvConfigStorageUri(uri: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(mpvConfigStorageUri = uri)
            }
        }
    }

    private fun updateMpvConf(content: String) {
        viewModelScope.launch {
            val parsedAlang = parseFirstLangFromMpvConf(content, "alang")
            val parsedSlang = parseFirstLangFromMpvConf(content, "slang")
            preferencesRepository.updatePlayerPreferences { current ->
                current.copy(
                    mpvConf = content,
                    preferredAudioLanguage = parsedAlang ?: current.preferredAudioLanguage,
                    preferredSubtitleLanguage = parsedSlang ?: current.preferredSubtitleLanguage,
                )
            }
        }
    }

    private fun updateInputConf(content: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(inputConf = content)
            }
        }
    }

    private fun parseFirstLangFromMpvConf(mpvConf: String, key: String): String? {
        val line = mpvConf.lineSequence()
            .map { it.substringBefore("#").trim() }
            .firstOrNull { it.startsWith("$key=", ignoreCase = true) }
            ?: return null
        val langs = line.substringAfter("=").split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return langs.firstOrNull()
    }

    fun buildSettingsXml(): String {
        val appPrefs = stateInternal.value.applicationPreferences
        val playerPrefs = stateInternal.value.playerPreferences
        val writer = StringWriter()
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        serializer.startTag(null, "map")

        fun writeString(name: String, value: String) {
            serializer.startTag(null, "string")
            serializer.attribute(null, "name", name)
            serializer.text(value)
            serializer.endTag(null, "string")
        }

        fun writeBoolean(name: String, value: Boolean) {
            serializer.startTag(null, "boolean")
            serializer.attribute(null, "name", name)
            serializer.attribute(null, "value", value.toString())
            serializer.endTag(null, "boolean")
        }

        fun writeInt(name: String, value: Int) {
            serializer.startTag(null, "int")
            serializer.attribute(null, "name", name)
            serializer.attribute(null, "value", value.toString())
            serializer.endTag(null, "int")
        }

        fun writeFloat(name: String, value: Float) {
            serializer.startTag(null, "float")
            serializer.attribute(null, "name", name)
            serializer.attribute(null, "value", value.toString())
            serializer.endTag(null, "float")
        }

        // ApplicationPreferences
        writeString("app_theme_config", appPrefs.themeConfig.name)
        writeBoolean("app_use_high_contrast_dark_theme", appPrefs.useHighContrastDarkTheme)
        writeBoolean("app_use_dynamic_colors", appPrefs.useDynamicColors)
        writeString("app_sort_by", appPrefs.sortBy.name)
        writeString("app_sort_order", appPrefs.sortOrder.name)
        writeString("app_media_view_mode", appPrefs.mediaViewMode.name)
        writeString("app_media_layout_mode", appPrefs.mediaLayoutMode.name)
        writeBoolean("app_mark_last_played_media", appPrefs.markLastPlayedMedia)
        writeBoolean("app_show_duration_field", appPrefs.showDurationField)
        writeBoolean("app_show_extension_field", appPrefs.showExtensionField)
        writeBoolean("app_show_path_field", appPrefs.showPathField)
        writeBoolean("app_show_resolution_field", appPrefs.showResolutionField)
        writeBoolean("app_show_size_field", appPrefs.showSizeField)
        writeBoolean("app_show_thumbnail_field", appPrefs.showThumbnailField)
        writeBoolean("app_show_played_progress", appPrefs.showPlayedProgress)

        // PlayerPreferences
        writeString("player_resume", playerPrefs.resume.name)
        writeBoolean("player_remember_brightness", playerPrefs.rememberPlayerBrightness)
        writeFloat("player_brightness", playerPrefs.playerBrightness)
        writeBoolean("player_remember_selections", playerPrefs.rememberSelections)
        writeString("player_screen_orientation", playerPrefs.playerScreenOrientation.name)
        writeString("player_video_zoom", playerPrefs.playerVideoZoom.name)
        writeFloat("player_default_playback_speed", playerPrefs.defaultPlaybackSpeed)
        writeBoolean("player_autoplay", playerPrefs.autoplay)
        writeBoolean("player_auto_pip", playerPrefs.autoPip)
        writeBoolean("player_auto_background_play", playerPrefs.autoBackgroundPlay)
        writeString("player_loop_mode", playerPrefs.loopMode.name)
        writeBoolean("player_enable_volume_swipe", playerPrefs.enableVolumeSwipeGesture)
        writeBoolean("player_enable_brightness_swipe", playerPrefs.enableBrightnessSwipeGesture)
        writeBoolean("player_use_seek_controls", playerPrefs.useSeekControls)
        writeBoolean("player_use_zoom_controls", playerPrefs.useZoomControls)
        writeBoolean("player_enable_pan_gesture", playerPrefs.enablePanGesture)
        writeString("player_double_tap_gesture", playerPrefs.doubleTapGesture.name)
        writeBoolean("player_use_long_press_controls", playerPrefs.useLongPressControls)
        writeFloat("player_long_press_speed", playerPrefs.longPressControlsSpeed)
        writeInt("player_seek_increment", playerPrefs.seekIncrement)
        writeFloat("player_seek_sensitivity", playerPrefs.seekSensitivity)
        writeFloat("player_volume_sensitivity", playerPrefs.volumeGestureSensitivity)
        writeFloat("player_brightness_sensitivity", playerPrefs.brightnessGestureSensitivity)
        writeInt("player_controller_timeout", playerPrefs.controllerAutoHideTimeout)
        writeString("player_control_buttons_position", playerPrefs.controlButtonsPosition.name)
        writeBoolean("player_use_material_you_controls", playerPrefs.useMaterialYouControls)
        writeBoolean("player_show_remaining_time", playerPrefs.showRemainingTime)
        writeBoolean("player_show_status_bar", playerPrefs.showStatusBar)
        writeBoolean("player_waving_timeline", playerPrefs.wavingTimeline)
        writeString("player_mpv_config_storage_uri", playerPrefs.mpvConfigStorageUri)
        writeString("player_mpv_conf", playerPrefs.mpvConf)
        writeString("player_input_conf", playerPrefs.inputConf)
        writeString("player_preferred_audio_language", playerPrefs.preferredAudioLanguage)
        writeBoolean("player_pause_on_headset_disconnect", playerPrefs.pauseOnHeadsetDisconnect)
        writeBoolean("player_require_audio_focus", playerPrefs.requireAudioFocus)
        writeBoolean("player_show_system_volume_panel", playerPrefs.showSystemVolumePanel)
        writeBoolean("player_enable_volume_boost", playerPrefs.enableVolumeBoost)
        writeBoolean("player_use_system_caption_style", playerPrefs.useSystemCaptionStyle)
        writeString("player_preferred_subtitle_language", playerPrefs.preferredSubtitleLanguage)
        writeString("player_subtitle_text_encoding", playerPrefs.subtitleTextEncoding)
        writeInt("player_subtitle_text_size", playerPrefs.subtitleTextSize)
        writeBoolean("player_subtitle_background", playerPrefs.subtitleBackground)
        writeString("player_subtitle_font", playerPrefs.subtitleFont.name)
        writeBoolean("player_subtitle_text_bold", playerPrefs.subtitleTextBold)
        writeBoolean("player_apply_embedded_styles", playerPrefs.applyEmbeddedStyles)
        writeString("player_subtitle_animation", playerPrefs.subtitleAnimation.name)
        writeInt("player_subtitle_bottom_padding", playerPrefs.subtitleBottomPadding)

        serializer.endTag(null, "map")
        serializer.endDocument()
        serializer.flush()
        return writer.toString()
    }

    private fun importSettingsXml(xmlContent: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching {
                val strings = mutableMapOf<String, String>()
                val booleans = mutableMapOf<String, Boolean>()
                val ints = mutableMapOf<String, Int>()
                val floats = mutableMapOf<String, Float>()

                val parser = Xml.newPullParser()
                parser.setInput(StringReader(xmlContent))
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tag = parser.name
                        val attrName = parser.getAttributeValue(null, "name")
                        val attrValue = parser.getAttributeValue(null, "value")
                        if (!attrName.isNullOrBlank()) {
                            when (tag) {
                                "string" -> strings[attrName] = parser.nextText()
                                "boolean" -> attrValue?.toBooleanStrictOrNull()?.let { booleans[attrName] = it }
                                "int" -> attrValue?.toIntOrNull()?.let { ints[attrName] = it }
                                "float" -> attrValue?.toFloatOrNull()?.let { floats[attrName] = it }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                if (strings.isEmpty() && booleans.isEmpty() && ints.isEmpty() && floats.isEmpty()) {
                    error("No valid XML entries found")
                }

                preferencesRepository.updateApplicationPreferences { app ->
                    app.copy(
                        themeConfig = strings["app_theme_config"]?.let { runCatching { ThemeConfig.valueOf(it) }.getOrNull() } ?: app.themeConfig,
                        useHighContrastDarkTheme = booleans["app_use_high_contrast_dark_theme"] ?: app.useHighContrastDarkTheme,
                        useDynamicColors = booleans["app_use_dynamic_colors"] ?: app.useDynamicColors,
                        sortBy = strings["app_sort_by"]?.let { runCatching { Sort.By.valueOf(it) }.getOrNull() } ?: app.sortBy,
                        sortOrder = strings["app_sort_order"]?.let { runCatching { Sort.Order.valueOf(it) }.getOrNull() } ?: app.sortOrder,
                        mediaViewMode = strings["app_media_view_mode"]?.let { runCatching { MediaViewMode.valueOf(it) }.getOrNull() } ?: app.mediaViewMode,
                        mediaLayoutMode = strings["app_media_layout_mode"]?.let { runCatching { MediaLayoutMode.valueOf(it) }.getOrNull() } ?: app.mediaLayoutMode,
                        markLastPlayedMedia = booleans["app_mark_last_played_media"] ?: app.markLastPlayedMedia,
                        showDurationField = booleans["app_show_duration_field"] ?: app.showDurationField,
                        showExtensionField = booleans["app_show_extension_field"] ?: app.showExtensionField,
                        showPathField = booleans["app_show_path_field"] ?: app.showPathField,
                        showResolutionField = booleans["app_show_resolution_field"] ?: app.showResolutionField,
                        showSizeField = booleans["app_show_size_field"] ?: app.showSizeField,
                        showThumbnailField = booleans["app_show_thumbnail_field"] ?: app.showThumbnailField,
                        showPlayedProgress = booleans["app_show_played_progress"] ?: app.showPlayedProgress,
                    )
                }

                preferencesRepository.updatePlayerPreferences { p ->
                    p.copy(
                        resume = strings["player_resume"]?.let { runCatching { Resume.valueOf(it) }.getOrNull() } ?: p.resume,
                        rememberPlayerBrightness = booleans["player_remember_brightness"] ?: p.rememberPlayerBrightness,
                        playerBrightness = floats["player_brightness"] ?: p.playerBrightness,
                        rememberSelections = booleans["player_remember_selections"] ?: p.rememberSelections,
                        playerScreenOrientation = strings["player_screen_orientation"]?.let { runCatching { ScreenOrientation.valueOf(it) }.getOrNull() } ?: p.playerScreenOrientation,
                        playerVideoZoom = strings["player_video_zoom"]?.let { runCatching { VideoContentScale.valueOf(it) }.getOrNull() } ?: p.playerVideoZoom,
                        defaultPlaybackSpeed = floats["player_default_playback_speed"] ?: p.defaultPlaybackSpeed,
                        autoplay = booleans["player_autoplay"] ?: p.autoplay,
                        autoPip = booleans["player_auto_pip"] ?: p.autoPip,
                        autoBackgroundPlay = booleans["player_auto_background_play"] ?: p.autoBackgroundPlay,
                        loopMode = strings["player_loop_mode"]?.let { runCatching { LoopMode.valueOf(it) }.getOrNull() } ?: p.loopMode,
                        enableVolumeSwipeGesture = booleans["player_enable_volume_swipe"] ?: p.enableVolumeSwipeGesture,
                        enableBrightnessSwipeGesture = booleans["player_enable_brightness_swipe"] ?: p.enableBrightnessSwipeGesture,
                        useSeekControls = booleans["player_use_seek_controls"] ?: p.useSeekControls,
                        useZoomControls = booleans["player_use_zoom_controls"] ?: p.useZoomControls,
                        enablePanGesture = booleans["player_enable_pan_gesture"] ?: p.enablePanGesture,
                        doubleTapGesture = strings["player_double_tap_gesture"]?.let { runCatching { DoubleTapGesture.valueOf(it) }.getOrNull() } ?: p.doubleTapGesture,
                        useLongPressControls = booleans["player_use_long_press_controls"] ?: p.useLongPressControls,
                        longPressControlsSpeed = floats["player_long_press_speed"] ?: p.longPressControlsSpeed,
                        seekIncrement = ints["player_seek_increment"] ?: p.seekIncrement,
                        seekSensitivity = floats["player_seek_sensitivity"] ?: p.seekSensitivity,
                        volumeGestureSensitivity = floats["player_volume_sensitivity"] ?: p.volumeGestureSensitivity,
                        brightnessGestureSensitivity = floats["player_brightness_sensitivity"] ?: p.brightnessGestureSensitivity,
                        controllerAutoHideTimeout = ints["player_controller_timeout"] ?: p.controllerAutoHideTimeout,
                        controlButtonsPosition = strings["player_control_buttons_position"]?.let { runCatching { ControlButtonsPosition.valueOf(it) }.getOrNull() } ?: p.controlButtonsPosition,
                        useMaterialYouControls = booleans["player_use_material_you_controls"] ?: p.useMaterialYouControls,
                        showRemainingTime = booleans["player_show_remaining_time"] ?: p.showRemainingTime,
                        showStatusBar = booleans["player_show_status_bar"] ?: p.showStatusBar,
                        wavingTimeline = booleans["player_waving_timeline"] ?: p.wavingTimeline,
                        mpvConfigStorageUri = strings["player_mpv_config_storage_uri"] ?: p.mpvConfigStorageUri,
                        mpvConf = strings["player_mpv_conf"] ?: p.mpvConf,
                        inputConf = strings["player_input_conf"] ?: p.inputConf,
                        preferredAudioLanguage = strings["player_preferred_audio_language"] ?: p.preferredAudioLanguage,
                        pauseOnHeadsetDisconnect = booleans["player_pause_on_headset_disconnect"] ?: p.pauseOnHeadsetDisconnect,
                        requireAudioFocus = booleans["player_require_audio_focus"] ?: p.requireAudioFocus,
                        showSystemVolumePanel = booleans["player_show_system_volume_panel"] ?: p.showSystemVolumePanel,
                        enableVolumeBoost = booleans["player_enable_volume_boost"] ?: p.enableVolumeBoost,
                        useSystemCaptionStyle = booleans["player_use_system_caption_style"] ?: p.useSystemCaptionStyle,
                        preferredSubtitleLanguage = strings["player_preferred_subtitle_language"] ?: p.preferredSubtitleLanguage,
                        subtitleTextEncoding = strings["player_subtitle_text_encoding"] ?: p.subtitleTextEncoding,
                        subtitleTextSize = ints["player_subtitle_text_size"] ?: p.subtitleTextSize,
                        subtitleBackground = booleans["player_subtitle_background"] ?: p.subtitleBackground,
                        subtitleFont = strings["player_subtitle_font"]?.let { runCatching { Font.valueOf(it) }.getOrNull() } ?: p.subtitleFont,
                        subtitleTextBold = booleans["player_subtitle_text_bold"] ?: p.subtitleTextBold,
                        applyEmbeddedStyles = booleans["player_apply_embedded_styles"] ?: p.applyEmbeddedStyles,
                        subtitleAnimation = strings["player_subtitle_animation"]?.let { runCatching { SubtitleAnimation.valueOf(it) }.getOrNull() } ?: p.subtitleAnimation,
                        subtitleBottomPadding = ints["player_subtitle_bottom_padding"] ?: p.subtitleBottomPadding,
                    )
                }
                true
            }.getOrDefault(false)
            onResult(success)
        }
    }
}

@Stable
data class GeneralPreferencesUiState(
    val showDialog: GeneralPreferencesDialog? = null,
    val playerPreferences: PlayerPreferences = PlayerPreferences(),
    val applicationPreferences: ApplicationPreferences = ApplicationPreferences(),
)

sealed interface GeneralPreferencesDialog {
    data object ClearThumbnailCacheDialog : GeneralPreferencesDialog
    data object ResetSettingsDialog : GeneralPreferencesDialog
    data object EditMpvConfDialog : GeneralPreferencesDialog
    data object EditInputConfDialog : GeneralPreferencesDialog
}

sealed interface GeneralPreferencesUiEvent {
    data object NavigateUp : GeneralPreferencesUiEvent

    data class ShowDialog(val value: GeneralPreferencesDialog?) : GeneralPreferencesUiEvent
    data object ClearThumbnailCache : GeneralPreferencesUiEvent
    data object ResetSettings : GeneralPreferencesUiEvent
    data class UpdateMpvConfigStorageUri(val uri: String) : GeneralPreferencesUiEvent
    data class UpdateMpvConf(val content: String) : GeneralPreferencesUiEvent
    data class UpdateInputConf(val content: String) : GeneralPreferencesUiEvent
    data class ImportSettingsXml(val xmlContent: String, val onResult: (Boolean) -> Unit) : GeneralPreferencesUiEvent
}
