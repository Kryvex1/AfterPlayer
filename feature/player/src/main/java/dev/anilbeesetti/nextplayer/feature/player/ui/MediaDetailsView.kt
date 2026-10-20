package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.MediaDetailsView(
    show: Boolean,
    player: Player,
    title: String,
    onDismiss: () -> Unit,
) {
    val durationMs = remember(player.duration) { player.duration.coerceAtLeast(0L) }
    val durationFormatted = remember(durationMs) { durationMs.milliseconds.formatted() }
    val videoSize = player.videoSize
    val realHeight = remember(videoSize) {
        val h = videoSize.height
        if (h > 0) "${h}p" else null
    }
    val resolutionLabel = realHeight
    val uriString = remember(player.currentMediaItem) {
        player.currentMediaItem?.localConfiguration?.uri?.path
            ?: player.currentMediaItem?.localConfiguration?.uri?.toString()
            ?: ""
    }

    OverlayView(
        show = show,
        title = stringResource(R.string.info),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Full title card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title.ifBlank { "Video" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (resolutionLabel != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = resolutionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Specs grid / details
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        DetailItem(
                            icon = NextIcons.Size,
                            label = "Resolution",
                            value = "${videoSize.width} × ${videoSize.height}",
                        )
                    }
                    if (durationMs > 0) {
                        DetailItem(
                            icon = NextIcons.History,
                            label = "Duration",
                            value = durationFormatted,
                        )
                    }
                    if (uriString.isNotBlank()) {
                        DetailItem(
                            icon = NextIcons.Folder,
                            label = "Source",
                            value = uriString.substringAfterLast("/"),
                        )
                    }
                    val audioCount = remember(player.currentTracks) {
                        player.currentTracks.groups.count { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
                    }
                    if (audioCount > 0) {
                        DetailItem(
                            icon = NextIcons.Audio,
                            label = "Audio Tracks",
                            value = "$audioCount tracks available",
                        )
                    }
                    val subtitleCount = remember(player.currentTracks) {
                        player.currentTracks.groups.count { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
                    }
                    if (subtitleCount > 0) {
                        DetailItem(
                            icon = NextIcons.Subtitle,
                            label = "Subtitles",
                            value = "$subtitleCount tracks available",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
