package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.feature.player.model.DecoderTrackType
import dev.anilbeesetti.nextplayer.feature.player.model.descriptionRes
import dev.anilbeesetti.nextplayer.feature.player.model.labelRes
import dev.anilbeesetti.nextplayer.feature.player.model.selectableDecoderModes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@Composable
fun BoxScope.DecoderSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    videoMode: DecoderMode?,
    audioMode: DecoderMode?,
    onVideoModeSelected: (DecoderMode) -> Unit,
    onAudioModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit = {},
) {
    var selectedTrackType by rememberSaveable { mutableStateOf(DecoderTrackType.VIDEO) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_decoders),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DecoderTrackTypeSelector(
                selectedTrackType = selectedTrackType,
                onTrackTypeSelected = { selectedTrackType = it },
            )
            DecoderModeGroup(
                trackType = selectedTrackType,
                selectedMode = when (selectedTrackType) {
                    DecoderTrackType.VIDEO -> videoMode
                    DecoderTrackType.AUDIO -> audioMode
                },
                onModeSelected = { mode ->
                    when (selectedTrackType) {
                        DecoderTrackType.VIDEO -> onVideoModeSelected(mode)
                        DecoderTrackType.AUDIO -> onAudioModeSelected(mode)
                    }
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun DecoderTrackTypeSelector(
    selectedTrackType: DecoderTrackType,
    onTrackTypeSelected: (DecoderTrackType) -> Unit,
) {
    val trackTypes = DecoderTrackType.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        trackTypes.forEachIndexed { index, trackType ->
            SegmentedButton(
                selected = selectedTrackType == trackType,
                onClick = { onTrackTypeSelected(trackType) },
                shape = SegmentedButtonDefaults.itemShape(index, trackTypes.size),
            ) {
                Text(
                    text = stringResource(
                        when (trackType) {
                            DecoderTrackType.VIDEO -> R.string.video
                            DecoderTrackType.AUDIO -> R.string.audio
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun DecoderModeGroup(
    trackType: DecoderTrackType,
    selectedMode: DecoderMode?,
    onModeSelected: (DecoderMode) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        selectableDecoderModes.forEach { mode ->
            DecoderModeRow(
                trackType = trackType,
                mode = mode,
                selected = mode == (selectedMode ?: DecoderMode.AUTO),
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun DecoderModeRow(
    trackType: DecoderTrackType,
    mode: DecoderMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val titleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleText = if (trackType == DecoderTrackType.AUDIO) {
        when (mode) {
            DecoderMode.AUTO -> "Auto (Default)"
            DecoderMode.HARDWARE -> "HW (System Audio)"
            DecoderMode.SOFTWARE -> "SW (Android Codec)"
            DecoderMode.FFMPEG -> "FFmpeg (Dolby / EAC3 / DTS)"
        }
    } else {
        stringResource(mode.labelRes)
    }
    val descText = if (trackType == DecoderTrackType.AUDIO) {
        when (mode) {
            DecoderMode.AUTO -> "Automatically selects best hardware or FFmpeg audio decoder"
            DecoderMode.HARDWARE -> "Direct device hardware audio decoder"
            DecoderMode.SOFTWARE -> "Standard Android software audio decoder"
            DecoderMode.FFMPEG -> "Built-in multi-channel audio engine for AC3, EAC3, DTS, AAC, TrueHD"
        }
    } else {
        stringResource(mode.descriptionRes)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
            )
            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
            )
        }
    }
}
