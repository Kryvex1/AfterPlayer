package dev.anilbeesetti.nextplayer.settings.screens.general

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.CancelButton
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun GeneralPreferencesScreen(
    viewModel: GeneralPreferencesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/xml"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val xml = viewModel.buildSettingsXml()
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(xml.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, R.string.settings_exported_successfully, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val xml = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!xml.isNullOrBlank()) {
                    viewModel.onAction(
                        GeneralPreferencesUiEvent.ImportSettingsXml(xml) { success ->
                            val msgRes = if (success) {
                                R.string.settings_imported_successfully
                            } else {
                                R.string.settings_import_failed
                            }
                            Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }

    val pickStorageLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.onAction(GeneralPreferencesUiEvent.UpdateMpvConfigStorageUri(uri.toString()))
        }
    }

    GeneralPreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onExportSettingsClick = { exportSettingsLauncher.launch("afterplayer_settings.xml") },
        onImportSettingsClick = { importSettingsLauncher.launch(arrayOf("text/xml", "application/xml", "*/*")) },
        onPickStorageLocationClick = { pickStorageLocationLauncher.launch(null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneralPreferencesScreenContent(
    state: GeneralPreferencesUiState,
    onAction: (GeneralPreferencesUiEvent) -> Unit,
    onExportSettingsClick: () -> Unit = {},
    onImportSettingsClick: () -> Unit = {},
    onPickStorageLocationClick: () -> Unit = {},
) {
    val listFocusRequester = rememberTvListFocusRequester()
    val storageUri = state.playerPreferences.mpvConfigStorageUri

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.advanced_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = { onAction(GeneralPreferencesUiEvent.NavigateUp) },
                        modifier = Modifier.tvFocusDown(listFocusRequester),
                    ) {
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.backup_and_restore))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(R.string.export_settings),
                    description = stringResource(R.string.export_settings_description),
                    icon = NextIcons.ArrowUpward,
                    onClick = onExportSettingsClick,
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.import_settings),
                    description = stringResource(R.string.import_settings_description),
                    icon = NextIcons.ArrowDownward,
                    onClick = onImportSettingsClick,
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.mpv_configuration))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceItem(
                    title = stringResource(R.string.pick_configuration_storage_location),
                    description = storageUri.takeIf { it.isNotBlank() },
                    enabled = true,
                    onClick = onPickStorageLocationClick,
                    isFirstItem = true,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VerticalDivider(
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            IconButton(
                                onClick = {
                                    onAction(GeneralPreferencesUiEvent.UpdateMpvConfigStorageUri(""))
                                },
                            ) {
                                Icon(
                                    imageVector = NextIcons.Close,
                                    contentDescription = stringResource(R.string.clear_font),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.edit_mpv_conf),
                    description = stringResource(R.string.tap_to_edit_configuration),
                    onClick = { onAction(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.EditMpvConfDialog)) },
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.edit_input_conf),
                    description = stringResource(R.string.tap_to_edit_configuration),
                    onClick = { onAction(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.EditInputConfDialog)) },
                    isLastItem = true,
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.history))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(R.string.delete_thumbnail_cache),
                    description = stringResource(R.string.delete_thumbnail_cache_description),
                    icon = NextIcons.DeleteSweep,
                    onClick = { onAction(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ClearThumbnailCacheDialog)) },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.reset_settings),
                    description = stringResource(R.string.reset_settings_description),
                    icon = NextIcons.History,
                    onClick = { onAction(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ResetSettingsDialog)) },
                    isLastItem = true,
                )
            }
        }

        state.showDialog?.let { dialog ->
            when (dialog) {
                GeneralPreferencesDialog.ClearThumbnailCacheDialog -> {
                    NextDialog(
                        onDismissRequest = { onAction(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.delete_thumbnail_cache),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onAction(GeneralPreferencesUiEvent.ClearThumbnailCache)
                                    onAction(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.delete))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onAction(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.delete_thumbnail_cache_confirmation),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }

                GeneralPreferencesDialog.ResetSettingsDialog -> {
                    NextDialog(
                        onDismissRequest = { onAction(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.reset_settings),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onAction(GeneralPreferencesUiEvent.ResetSettings)
                                    onAction(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.reset))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onAction(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.reset_settings_confirmation),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }

                GeneralPreferencesDialog.EditMpvConfDialog -> {
                    MpvConfEditorDialog(
                        initialContent = state.playerPreferences.mpvConf,
                        onDismiss = { onAction(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        onSave = { updated ->
                            onAction(GeneralPreferencesUiEvent.UpdateMpvConf(updated))
                            onAction(GeneralPreferencesUiEvent.ShowDialog(null))
                        },
                    )
                }

                GeneralPreferencesDialog.EditInputConfDialog -> {
                    InputConfEditorDialog(
                        initialContent = state.playerPreferences.inputConf,
                        onDismiss = { onAction(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        onSave = { updated ->
                            onAction(GeneralPreferencesUiEvent.UpdateInputConf(updated))
                            onAction(GeneralPreferencesUiEvent.ShowDialog(null))
                        },
                    )
                }
            }
        }
    }
}

private data class LanguagePreset(
    val label: String,
    val codes: List<String>,
)

private val LANGUAGE_PRESETS = listOf(
    LanguagePreset("Hindi (hi,hin)", listOf("hi", "hin")),
    LanguagePreset("English (en,eng)", listOf("en", "eng")),
    LanguagePreset("Japanese (ja,jpn)", listOf("ja", "jpn")),
    LanguagePreset("Korean (ko,kor)", listOf("ko", "kor")),
    LanguagePreset("Tamil (ta,tam)", listOf("ta", "tam")),
    LanguagePreset("Telugu (te,tel)", listOf("te", "tel")),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MpvConfEditorDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var content by rememberSaveable { mutableStateOf(initialContent) }
    var targetKey by rememberSaveable { mutableStateOf("alang") }

    val currentKeyLangs = remember(content, targetKey) {
        val line = content.lineSequence()
            .map { it.substringBefore("#").trim() }
            .firstOrNull { it.startsWith("$targetKey=", ignoreCase = true) }
        line?.substringAfter("=")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    }

    fun updateKeyLangs(key: String, newLangs: List<String>) {
        val newLine = "$key=${newLangs.joinToString(",")}"
        val lines = content.lines().toMutableList()
        val index = lines.indexOfFirst { it.substringBefore("#").trim().startsWith("$key=", ignoreCase = true) }
        if (index >= 0) {
            lines[index] = newLine
        } else {
            lines.add(0, newLine)
        }
        content = lines.joinToString("\n")
    }

    NextDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_mpv_conf),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(content) }) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.mpv_conf_quick_presets),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = targetKey == "alang",
                        onClick = { targetKey = "alang" },
                        label = { Text("Audio (alang)") },
                    )
                    FilterChip(
                        selected = targetKey == "slang",
                        onClick = { targetKey = "slang" },
                        label = { Text("Subtitles (slang)") },
                    )
                }

                if (currentKeyLangs.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentKeyLangs.forEachIndexed { index, code ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = "${index + 1}. $code",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val mutable = currentKeyLangs.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                updateKeyLangs(targetKey, mutable)
                                            },
                                            modifier = Modifier.size(22.dp),
                                        ) {
                                            Icon(
                                                imageVector = NextIcons.ArrowUpward,
                                                contentDescription = "Move Up",
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                    if (index < currentKeyLangs.lastIndex) {
                                        IconButton(
                                            onClick = {
                                                val mutable = currentKeyLangs.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                updateKeyLangs(targetKey, mutable)
                                            },
                                            modifier = Modifier.size(22.dp),
                                        ) {
                                            Icon(
                                                imageVector = NextIcons.ArrowDownward,
                                                contentDescription = "Move Down",
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val mutable = currentKeyLangs.toMutableList()
                                            mutable.removeAt(index)
                                            updateKeyLangs(targetKey, mutable)
                                        },
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Icon(
                                            imageVector = NextIcons.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LANGUAGE_PRESETS.forEach { preset ->
                        AssistChip(
                            onClick = {
                                val updated = (currentKeyLangs + preset.codes).distinct()
                                updateKeyLangs(targetKey, updated)
                            },
                            label = { Text("+ ${preset.label}", fontSize = 12.sp) },
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    minLines = 8,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    placeholder = {
                        Text(
                            text = "alang=hi,hin,en,eng\nslang=hi,hin,en,eng\nhwdec=auto-safe",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputConfEditorDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var content by rememberSaveable { mutableStateOf(initialContent) }
    val presets = remember {
        listOf(
            "RIGHT seek 5",
            "LEFT seek -5",
            "UP add volume 5",
            "DOWN add volume -5",
            "SPACE cycle pause",
            "j cycle sub",
            "# cycle audio",
        )
    }

    NextDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_input_conf),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(content) }) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                content = if (content.isBlank()) preset else "$content\n$preset"
                            },
                            label = { Text("+ $preset", fontSize = 12.sp) },
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    minLines = 8,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                )
            }
        },
    )
}
