package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.SubtitleAnimation
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.CustomFontConfig
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name
import dev.anilbeesetti.nextplayer.settings.utils.LocalesHelper
import java.nio.charset.Charset

@Composable
fun SubtitlePreferencesScreen(
    viewModel: SubtitlePreferencesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SubtitlePreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubtitlePreferencesScreenContent(
    state: SubtitlePreferencesUiState,
    onAction: (SubtitlePreferencesUiEvent) -> Unit,
) {
    val languages = remember { listOf(Pair("None", "")) + LocalesHelper.getAvailableLocales() }
    val charsetResource = stringArrayResource(id = R.array.charsets_list)
    val context = LocalContext.current

    val regularFontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            onAction(SubtitlePreferencesUiEvent.UpdateCustomRegularFont(it.toString()))
        }
    }

    val boldFontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            onAction(SubtitlePreferencesUiEvent.UpdateCustomBoldFont(it.toString()))
        }
    }

    val hindiFontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            onAction(SubtitlePreferencesUiEvent.UpdateCustomHindiFont(it.toString()))
        }
    }

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.subtitle),
                navigationIcon = {
                    FilledTonalIconButton(onClick = { onAction(SubtitlePreferencesUiEvent.NavigateUp) }, modifier = Modifier.tvFocusDown(listFocusRequester)) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(listFocusRequester)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_subtitle_lang),
                    description = LocalesHelper.getLocaleDisplayLanguage(state.preferences.preferredSubtitleLanguage)
                        .takeIf { it.isNotBlank() } ?: stringResource(R.string.preferred_subtitle_lang_description),
                    icon = NextIcons.Language,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleLanguageDialog)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.subtitle_text_encoding),
                    description = charsetResource.first { it.contains(state.preferences.subtitleTextEncoding) },
                    icon = NextIcons.Subtitle,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleEncodingDialog)) },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.appearance_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.system_caption_style),
                    description = stringResource(R.string.system_caption_style_desc),
                    icon = NextIcons.Caption,
                    isChecked = state.preferences.useSystemCaptionStyle,
                    onChecked = { onAction(SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle) },
                    onClick = { context.startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_font),
                    description = state.preferences.subtitleFont.name(),
                    icon = NextIcons.Font,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleFontDialog)) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_text_bold),
                    description = stringResource(id = R.string.subtitle_text_bold_desc),
                    icon = NextIcons.Bold,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    isChecked = state.preferences.subtitleTextBold,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleSubtitleTextBold) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_text_size),
                    description = "${state.preferences.subtitleTextSize} sp",
                    icon = NextIcons.FontSize,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    value = state.preferences.subtitleTextSize.toFloat(),
                    valueRange = 8f..36f,
                    onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(it.toInt())) },
                    trailingContent = {
                        FilledIconButton(
                            enabled = state.preferences.useSystemCaptionStyle.not(),
                            onClick = {
                                onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE))
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_increment),
                            )
                        }
                    },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_bottom_padding),
                    description = "${state.preferences.subtitleBottomPadding} dp",
                    icon = NextIcons.Length,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    value = state.preferences.subtitleBottomPadding.toFloat(),
                    valueRange = 0f..80f,
                    onValueChange = { onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBottomPadding(it.toInt())) },
                    trailingContent = {
                        FilledIconButton(
                            enabled = state.preferences.useSystemCaptionStyle.not(),
                            onClick = {
                                onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBottomPadding(PlayerPreferences.DEFAULT_SUBTITLE_BOTTOM_PADDING))
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_increment),
                            )
                        }
                    },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_animation),
                    description = when (state.preferences.subtitleAnimation) {
                        SubtitleAnimation.NONE -> stringResource(R.string.subtitle_animation_none)
                        SubtitleAnimation.FADE -> stringResource(R.string.subtitle_animation_fade)
                        SubtitleAnimation.TYPEWRITER -> stringResource(R.string.subtitle_animation_typewriter)
                    },
                    icon = NextIcons.Speed,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    onClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleAnimationDialog)) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_background),
                    description = stringResource(id = R.string.subtitle_background_desc),
                    icon = NextIcons.Background,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    isChecked = state.preferences.subtitleBackground,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleSubtitleBackground) },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.embedded_styles),
                    description = stringResource(R.string.embedded_styles_desc),
                    icon = NextIcons.Style,
                    isChecked = state.preferences.applyEmbeddedStyles,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles) },
                    isLastItem = true,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Font Dual-Variant & Hindi Pickers
            ListSectionTitle(text = stringResource(id = R.string.font_source_custom))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.custom_font_single_file_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // Regular / Light Font Picker
                    val regularName = CustomFontConfig.getFileNameFromUri(context, state.preferences.customRegularFontUri)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.custom_regular_font),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = regularName ?: stringResource(R.string.custom_regular_font_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (regularName != null) {
                            IconButton(onClick = { onAction(SubtitlePreferencesUiEvent.UpdateCustomRegularFont(null)) }) {
                                Icon(NextIcons.Close, contentDescription = stringResource(R.string.clear_font))
                            }
                        }
                        OutlinedButton(
                            onClick = { regularFontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream", "*/*")) },
                        ) {
                            Text(if (regularName != null) "Change" else "Select")
                        }
                    }

                    // Bold Font Picker
                    val boldName = CustomFontConfig.getFileNameFromUri(context, state.preferences.customBoldFontUri)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.custom_bold_font),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = boldName ?: stringResource(R.string.custom_bold_font_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (boldName != null) {
                            IconButton(onClick = { onAction(SubtitlePreferencesUiEvent.UpdateCustomBoldFont(null)) }) {
                                Icon(NextIcons.Close, contentDescription = stringResource(R.string.clear_font))
                            }
                        }
                        OutlinedButton(
                            onClick = { boldFontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream", "*/*")) },
                        ) {
                            Text(if (boldName != null) "Change" else "Select")
                        }
                    }

                    // Hindi Font Picker
                    val hindiName = CustomFontConfig.getFileNameFromUri(context, state.preferences.customHindiFontUri)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.hindi_font),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = hindiName ?: stringResource(R.string.hindi_font_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (hindiName != null) {
                            IconButton(onClick = { onAction(SubtitlePreferencesUiEvent.UpdateCustomHindiFont(null)) }) {
                                Icon(NextIcons.Close, contentDescription = stringResource(R.string.clear_font))
                            }
                        }
                        OutlinedButton(
                            onClick = { hindiFontPickerLauncher.launch(arrayOf("font/*", "application/octet-stream", "*/*")) },
                        ) {
                            Text(if (hindiName != null) "Change" else "Select")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Font Preview Card
            ListSectionTitle(text = stringResource(id = R.string.font_preview))
            val previewFontFamily = remember(
                state.preferences.customRegularFontUri,
                state.preferences.customBoldFontUri,
            ) {
                CustomFontConfig(
                    regularFontUri = state.preferences.customRegularFontUri,
                    boldFontUri = state.preferences.customBoldFontUri,
                ).resolveFontFamily(context)
            }
            val previewHindiFontFamily = remember(state.preferences.customHindiFontUri) {
                CustomFontConfig.resolveHindiFontFamily(context, state.preferences.customHindiFontUri)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "English Regular: The quick brown fox jumps over the lazy dog.",
                        fontFamily = previewFontFamily,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "English Bold: The quick brown fox jumps over the lazy dog.",
                        fontFamily = previewFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "हिन्दी नियमित: यह एक सुंदर वीडियो प्लेयर है।",
                        fontFamily = previewHindiFontFamily,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "हिन्दी बोल्ड: उच्च गुणवत्ता वाले उपशीर्षक और फ़ॉन्ट।",
                        fontFamily = previewHindiFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                SubtitlePreferenceDialog.SubtitleLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_subtitle_lang),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == state.preferences.preferredSubtitleLanguage,
                                onClick = {
                                    onAction(SubtitlePreferencesUiEvent.UpdateSubtitleLanguage(it.second))
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleFontDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_font),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(Font.entries.toTypedArray()) {
                            RadioTextButton(
                                text = if (it == Font.DEFAULT) "SF Pro (Default)" else it.name(),
                                selected = it == state.preferences.subtitleFont,
                                onClick = {
                                    onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFont(it))
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleAnimationDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_animation),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(SubtitleAnimation.entries.toTypedArray()) {
                            RadioTextButton(
                                text = when (it) {
                                    SubtitleAnimation.NONE -> stringResource(R.string.subtitle_animation_none)
                                    SubtitleAnimation.FADE -> stringResource(R.string.subtitle_animation_fade)
                                    SubtitleAnimation.TYPEWRITER -> stringResource(R.string.subtitle_animation_typewriter)
                                },
                                selected = it == state.preferences.subtitleAnimation,
                                onClick = {
                                    onAction(SubtitlePreferencesUiEvent.UpdateSubtitleAnimation(it))
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleEncodingDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_text_encoding),
                        onDismissClick = { onAction(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(charsetResource) {
                            val currentCharset = it.substringAfterLast("(", "").removeSuffix(")")
                            if (currentCharset.isEmpty() || Charset.isSupported(currentCharset)) {
                                RadioTextButton(
                                    text = it,
                                    selected = currentCharset == state.preferences.subtitleTextEncoding,
                                    onClick = {
                                        onAction(SubtitlePreferencesUiEvent.UpdateSubtitleEncoding(currentCharset))
                                        onAction(SubtitlePreferencesUiEvent.ShowDialog(null))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
