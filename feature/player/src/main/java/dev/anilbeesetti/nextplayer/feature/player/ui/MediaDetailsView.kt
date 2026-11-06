package dev.anilbeesetti.nextplayer.feature.player.ui

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import io.github.anilbeesetti.nextlib.mediainfo.MediaInfoBuilder
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.MediaDetailsView(
    show: Boolean,
    player: Player,
    title: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val durationMs = remember(player.duration) { player.duration.coerceAtLeast(0L) }
    val durationFormatted = remember(durationMs) { durationMs.milliseconds.formatted() }
    val videoSize = player.videoSize
    val resolutionLabel = remember(videoSize) {
        val w = videoSize.width
        val h = videoSize.height
        val shortEdge = if (w > 0 && h > 0) minOf(w, h) else h
        if (shortEdge > 0) "${shortEdge}p" else null
    }
    val trackFrameRate = remember(player.currentTracks) {
        val videoTrackGroup = player.currentTracks.groups.firstOrNull {
            it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO && it.isSelected
        } ?: player.currentTracks.groups.firstOrNull {
            it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO
        }
        (0 until (videoTrackGroup?.length ?: 0))
            .map { videoTrackGroup!!.getTrackFormat(it).frameRate }
            .firstOrNull { it > 0f }
    }
    val resolvedFrameRate by produceState<Float?>(
        initialValue = trackFrameRate,
        key1 = player.currentMediaItem?.mediaId,
        key2 = trackFrameRate,
        key3 = show,
    ) {
        if (trackFrameRate != null && trackFrameRate > 0f) {
            value = trackFrameRate
            return@produceState
        }
        if (!show) return@produceState
        val mediaItem = player.currentMediaItem ?: return@produceState
        val uri = mediaItem.localConfiguration?.uri
            ?: mediaItem.mediaId.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: return@produceState

        value = withContext(Dispatchers.IO) {
            // 1. Fast check via Android MediaExtractor
            val extractorFps = runCatching {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(context, uri, null)
                    var fps: Float? = null
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                        if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            fps = runCatching { format.getFloat(MediaFormat.KEY_FRAME_RATE) }
                                .getOrElse { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                            if (fps != null && fps > 0f) break
                        }
                    }
                    fps
                } finally {
                    extractor.release()
                }
            }.getOrNull()

            if (extractorFps != null && extractorFps > 0f) {
                return@withContext extractorFps
            }

            // 2. Reliable FFmpeg MediaInfoBuilder fallback for MKV/WebM/MP4 containers
            runCatching {
                val mediaInfo = MediaInfoBuilder().from(context = context, uri = uri).build()
                try {
                    mediaInfo?.videoStream?.frameRate?.toFloat()?.takeIf { it > 0f }
                } finally {
                    mediaInfo?.release()
                }
            }.getOrNull()
        }
    }
    val frameRateFormatted = remember(resolvedFrameRate) {
        val fps = resolvedFrameRate
        if (fps != null && fps > 0f) {
            val rounded = round(fps * 100f) / 100f
            if (rounded % 1f == 0f) {
                "${rounded.toInt()} FPS"
            } else {
                "$rounded FPS"
            }
        } else {
            null
        }
    }
    val videoCodecLabel = remember(player.currentTracks) {
        val videoTrackGroup = player.currentTracks.groups.firstOrNull {
            it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO && it.isSelected
        } ?: player.currentTracks.groups.firstOrNull {
            it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO
        }
        val format = if ((videoTrackGroup?.length ?: 0) > 0) videoTrackGroup?.getTrackFormat(0) else null
        format?.sampleMimeType?.substringAfter("video/")?.uppercase()
    }
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
            // Full title card with Quality & FPS badges
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
                    if (resolutionLabel != null || frameRateFormatted != null || videoCodecLabel != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (resolutionLabel != null) {
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
                            if (frameRateFormatted != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = frameRateFormatted,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            if (videoCodecLabel != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = videoCodecLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
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
                            label = stringResource(R.string.resolution),
                            value = "${videoSize.width} × ${videoSize.height}",
                        )
                    }
                    if (frameRateFormatted != null) {
                        DetailItem(
                            icon = NextIcons.Speed,
                            label = stringResource(R.string.frame_rate),
                            value = frameRateFormatted,
                        )
                    }
                    if (videoCodecLabel != null) {
                        DetailItem(
                            icon = NextIcons.Video,
                            label = stringResource(R.string.codec),
                            value = videoCodecLabel,
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
