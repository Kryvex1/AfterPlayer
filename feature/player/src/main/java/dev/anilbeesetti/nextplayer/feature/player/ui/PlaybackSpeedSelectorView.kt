package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextSwitch
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPlaybackParametersState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.PlaybackSpeedSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val playbackParametersState = rememberPlaybackParametersState(player)

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.select_playback_speed),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val currentSpeed = playbackParametersState.speed
            val minSpeed = 0.25f
            val maxSpeed = 4.0f
            val step = 0.05f

            // 1. Unified Compact Speed Controller Card (Value + Stepper + Custom Sleek Track)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val newSpeed = (currentSpeed - step).coerceAtLeast(minSpeed)
                                val rounded = (round(newSpeed * 20f) / 20f)
                                playbackParametersState.setPlaybackSpeed(rounded)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_remove),
                                contentDescription = stringResource(R.string.playback_speed_slower),
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f×", currentSpeed),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            val isNormal = abs(currentSpeed - 1.0f) < 0.01f
                            if (!isNormal) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playbackParametersState.setPlaybackSpeed(1.0f)
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_reset),
                                            contentDescription = stringResource(R.string.reset_speed),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            text = "1.0×",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = {
                                val newSpeed = (currentSpeed + step).coerceAtMost(maxSpeed)
                                val rounded = (round(newSpeed * 20f) / 20f)
                                playbackParametersState.setPlaybackSpeed(rounded)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = stringResource(R.string.playback_speed_faster),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Custom Ultra-Clean Pill Track Slider (No ugly gaps or tick dots)
                    CleanSpeedSliderTrack(
                        value = currentSpeed.coerceIn(minSpeed, maxSpeed),
                        minSpeed = minSpeed,
                        maxSpeed = maxSpeed,
                        onValueChange = { rawValue ->
                            val snapped = (round(rawValue * 20f) / 20f).coerceIn(minSpeed, maxSpeed)
                            if (abs(snapped - currentSpeed) >= 0.05f) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            playbackParametersState.setPlaybackSpeed(snapped)
                        },
                    )
                }
            }

            // 2. Compact Preset Speed Pills (Single Row)
            val presetSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presetSpeeds.forEach { speed ->
                    val isSelected = abs(currentSpeed - speed) < 0.01f
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                playbackParametersState.setPlaybackSpeed(speed)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (speed == 1.0f) "1.0×" else "${speed}×",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            // 3. Skip Silence Toggle Option Card (Fits in the same screen without scrolling)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .toggleable(
                        value = playbackParametersState.skipSilenceEnabled,
                        onValueChange = { playbackParametersState.setIsSkipSilenceEnabled(it) },
                        role = Role.Switch,
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = NextIcons.Audio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.skip_silence),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.skip_silence_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    NextSwitch(
                        checked = playbackParametersState.skipSilenceEnabled,
                        onCheckedChange = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanSpeedSliderTrack(
    value: Float,
    minSpeed: Float,
    maxSpeed: Float,
    onValueChange: (Float) -> Unit,
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val thumbSize = 18.dp
    val thumbRadiusPx = with(density) { (thumbSize / 2).toPx() }
    val fraction = ((value - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0f, 1f)

    fun updateFromX(xPx: Float) {
        if (trackWidthPx <= 0) return
        val newFraction = (xPx / trackWidthPx.toFloat()).coerceIn(0f, 1f)
        val newValue = minSpeed + newFraction * (maxSpeed - minSpeed)
        onValueChange(newValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(minSpeed, maxSpeed) {
                    detectTapGestures { offset ->
                        updateFromX(offset.x)
                    }
                }
                .pointerInput(minSpeed, maxSpeed) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        updateFromX(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Unfilled background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                // Active filled track
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            // Sleek circular thumb
            if (trackWidthPx > 0) {
                val thumbOffsetX = (fraction * trackWidthPx - thumbRadiusPx)
                    .roundToInt()
                    .coerceIn(0, (trackWidthPx - thumbRadiusPx * 2).roundToInt().coerceAtLeast(0))
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetX, 0) }
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "0.25×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
                text = "1.0×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
                text = "4.0×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
