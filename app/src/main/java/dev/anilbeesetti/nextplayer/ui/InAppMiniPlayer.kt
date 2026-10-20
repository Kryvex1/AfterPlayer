package dev.anilbeesetti.nextplayer.ui

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.feature.player.PlayerActivity
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun InAppMiniPlayer(
    modifier: Modifier = Modifier,
    controller: MediaController?,
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(controller?.isPlaying == true) }
    var currentItem by remember { mutableStateOf<MediaItem?>(controller?.currentMediaItem) }
    var playbackState by remember { mutableStateOf(controller?.playbackState ?: Player.STATE_IDLE) }
    var positionMs by remember { mutableLongStateOf(controller?.currentPosition ?: 0L) }
    var durationMs by remember { mutableLongStateOf(controller?.duration ?: 0L) }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose {}

        isPlaying = controller.isPlaying
        currentItem = controller.currentMediaItem
        playbackState = controller.playbackState

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentItem = mediaItem
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
        }
        controller.addListener(listener)
        onDispose {
            controller.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && controller != null) {
            positionMs = controller.currentPosition
            durationMs = controller.duration
            delay(500)
        }
    }

    val visible = controller != null &&
        currentItem != null &&
        playbackState != Player.STATE_IDLE &&
        playbackState != Player.STATE_ENDED

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        ) + scaleIn(
            initialScale = 0.88f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(durationMillis = 250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        ) + scaleOut(
            targetScale = 0.88f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        ) + fadeOut(tween(durationMillis = 200)),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        data = currentItem?.localConfiguration?.uri
                    }
                    context.startActivity(intent)
                    if (context is android.app.Activity) {
                        context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = NextIcons.Video,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val title = currentItem?.mediaMetadata?.title?.toString()
                            ?: currentItem?.localConfiguration?.uri?.lastPathSegment
                            ?: stringResource(R.string.app_name)
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isPlaying) "Playing" else "Paused",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Play / Pause Button
                    IconButton(
                        onClick = {
                            if (controller?.isPlaying == true) {
                                controller.pause()
                            } else {
                                controller?.play()
                            }
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) NextIcons.Pause else NextIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Close Button
                    IconButton(
                        onClick = {
                            controller?.stop()
                            controller?.clearMediaItems()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = NextIcons.Close,
                            contentDescription = stringResource(R.string.dismiss),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Progress Bar at bottom of card
                if (durationMs > 0L) {
                    val progress = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
