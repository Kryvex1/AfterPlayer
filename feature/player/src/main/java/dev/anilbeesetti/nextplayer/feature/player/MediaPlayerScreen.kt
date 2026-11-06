package dev.anilbeesetti.nextplayer.feature.player

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.extensions.noRippleClickable
import dev.anilbeesetti.nextplayer.feature.player.extensions.toggleSystemBars
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.PlayerOrientationEffect
import dev.anilbeesetti.nextplayer.feature.player.state.rememberBrightnessState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberErrorState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberMediaPresentationState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberSeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberTapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVolumeAndBrightnessGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVolumeState
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerErrorDialogs
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerGestureIndicators
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerGestures
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerVerticalGestureIndicators
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.ui.preview.rememberPreviewPlayer
import kotlin.time.Duration.Companion.seconds

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }
val LocalUseMaterialYouControls = compositionLocalOf { false }

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    viewModel: PlayerViewModel,
    player: Player?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MediaPlayerContent(
        player = player,
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(UnstableApi::class)
@Composable
internal fun MediaPlayerContent(
    player: Player?,
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
) {
    val playerPreferences = state.playerPreferences
    val isPreview = LocalInspectionMode.current
    // Layout previews have neither a PlayerActivity nor a live video/audio session.
    val volumeState = if (!isPreview) {
        rememberVolumeState(
            player = player,
            showVolumePanelIfHeadsetIsOn = playerPreferences.showSystemVolumePanel,
        )
    } else {
        null
    }
    if (player == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val context = LocalContext.current
    val activity = LocalActivity.current
    if (!isPreview && volumeState != null) {
        DisposableEffect(activity, volumeState) {
            val playerActivity = activity as? PlayerActivity
            playerActivity?.onVolumeKeyPressed = { keyCode ->
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                        volumeState.increaseVolume(showVolumePanel = false)
                        true
                    }
                    android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        volumeState.decreaseVolume(showVolumePanel = false)
                        true
                    }
                    android.view.KeyEvent.KEYCODE_VOLUME_MUTE -> {
                        volumeState.updateVolumePercentage(0)
                        true
                    }
                    else -> false
                }
            }
            onDispose {
                playerActivity?.onVolumeKeyPressed = null
            }
        }
    }
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = playerPreferences.controllerAutoHideTimeout.seconds,
    )
    if (!isPreview) {
        LaunchedEffect(controlsVisibilityState.controlsVisible, playerPreferences.showStatusBar) {
            activity?.toggleSystemBars(
                showBars = controlsVisibilityState.controlsVisible && playerPreferences.showStatusBar,
                types = WindowInsetsCompat.Type.statusBars(),
            )
        }
    }
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = playerPreferences.doubleTapGesture,
        seekIncrementMillis = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        useLongPressGesture = playerPreferences.useLongPressControls,
        longPressSpeed = playerPreferences.longPressControlsSpeed,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = playerPreferences.seekSensitivity,
        enableSeekGesture = playerPreferences.useSeekControls,
    )
    val pictureInPictureState = if (!isPreview) {
        rememberPictureInPictureState(
            player = player,
            autoEnter = playerPreferences.autoPip,
        )
    } else {
        null
    }
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = playerPreferences.playerVideoZoom,
        enableZoomGesture = playerPreferences.useZoomControls,
        enablePanGesture = playerPreferences.enablePanGesture,
        onEvent = { onAction(PlayerAction.OnVideoZoomEvent(it)) },
    )

    val brightnessState = if (!isPreview) rememberBrightnessState() else null
    val volumeAndBrightnessGestureState = if (volumeState != null && brightnessState != null) {
        rememberVolumeAndBrightnessGestureState(
            volumeState = volumeState,
            brightnessState = brightnessState,
            enableVolumeGesture = playerPreferences.enableVolumeSwipeGesture,
            enableBrightnessGesture = playerPreferences.enableBrightnessSwipeGesture,
            volumeGestureSensitivity = playerPreferences.volumeGestureSensitivity,
            brightnessGestureSensitivity = playerPreferences.brightnessGestureSensitivity,
        )
    } else {
        null
    }
    if (!isPreview) {
        PlayerOrientationEffect(
            player = player,
            screenOrientation = playerPreferences.playerScreenOrientation,
        )
    }
    val errorState = rememberErrorState(player = player)

    LaunchedEffect(pictureInPictureState?.isInPictureInPictureMode) {
        if (pictureInPictureState?.isInPictureInPictureMode == true) controlsVisibilityState.hideControls()
    }
    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) controlsVisibilityState.hideControls()
    }
    if (brightnessState != null) {
        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            if (playerPreferences.rememberPlayerBrightness) {
                brightnessState.setBrightness(playerPreferences.playerBrightness)
            }
        }
        LaunchedEffect(brightnessState.currentBrightness) {
            if (playerPreferences.rememberPlayerBrightness) {
                onAction(PlayerAction.UpdatePlayerBrightness(brightnessState.currentBrightness))
            }
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val mediaPresentationState = rememberMediaPresentationState(player)
    var overlayView by remember { mutableStateOf<OverlayView?>(null) }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSidePanelOpen = overlayView != null && isLandscape
    val videoShrinkProgress by animateFloatAsState(
        targetValue = if (isSidePanelOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "video_shrink_progress",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E)),
    ) {
        if (pictureInPictureState != null) {
            val scale = 1f - (0.46f * videoShrinkProgress)
            val cornerRadius = (20f * videoShrinkProgress).dp
            val leftOffsetPx = with(density) { (16.dp * videoShrinkProgress).toPx() }
            val shadowElevationPx = with(density) { (16.dp * videoShrinkProgress).toPx() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        translationX = leftOffsetPx
                        shape = RoundedCornerShape(cornerRadius)
                        clip = videoShrinkProgress > 0.01f
                        shadowElevation = shadowElevationPx
                    }
                    .background(Color.Black),
            ) {
                PlayerContentFrame(
                    player = player,
                    pictureInPictureState = pictureInPictureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    subtitleConfiguration = SubtitleConfiguration(
                        useSystemCaptionStyle = playerPreferences.useSystemCaptionStyle,
                        showBackground = playerPreferences.subtitleBackground,
                        font = playerPreferences.subtitleFont,
                        textSize = playerPreferences.subtitleTextSize,
                        textBold = playerPreferences.subtitleTextBold || playerPreferences.subtitleBold,
                        applyEmbeddedStyles = playerPreferences.applyEmbeddedStyles,
                        customRegularFontUri = playerPreferences.customRegularFontUri,
                        customBoldFontUri = playerPreferences.customBoldFontUri,
                        customHindiFontUri = playerPreferences.customHindiFontUri,
                        animation = playerPreferences.subtitleAnimation,
                        bottomPadding = playerPreferences.subtitleBottomPadding,
                        controlsVisible = controlsVisibilityState.controlsVisible,
                    ),
                )
                if (mediaPresentationState.isBuffering && !seekGestureState.isSeeking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(68.dp),
                    )
                }
                PlayerGestureIndicators(tapGestureState)
                if (volumeAndBrightnessGestureState != null) {
                    PlayerGestures(
                        controlsVisibilityState = controlsVisibilityState,
                        tapGestureState = tapGestureState,
                        pictureInPictureState = pictureInPictureState,
                        seekGestureState = seekGestureState,
                        videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                        volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                    )
                }
                if (overlayView != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .noRippleClickable {
                                overlayView = null
                                controlsVisibilityState.hideControls()
                            },
                    )
                }
            }
        }
        MediaPlayerControls(
            player = player,
            state = state,
            onAction = onAction,
            controlsVisibilityState = controlsVisibilityState,
            tapGestureState = tapGestureState,
            seekGestureState = seekGestureState,
            videoZoomAndContentScaleState = videoZoomAndContentScaleState,
            isPipSupported = pictureInPictureState?.isPipSupported == true,
            overlayView = overlayView,
            onOverlayChange = { overlayView = it },
            onPictureInPictureClick = {
                pictureInPictureState?.let {
                    if (!it.hasPipPermission) {
                        Toast.makeText(context, R.string.enable_pip_from_settings, Toast.LENGTH_SHORT).show()
                        it.openPictureInPictureSettings()
                    } else {
                        it.enterPictureInPictureMode()
                    }
                }
            },
        )
        if (volumeAndBrightnessGestureState != null && volumeState != null && brightnessState != null) {
            val activeGestureOrKey = volumeAndBrightnessGestureState.activeGesture
                ?: if (volumeState.showVolumeIndicator) dev.anilbeesetti.nextplayer.feature.player.state.VerticalGesture.VOLUME else null
            PlayerVerticalGestureIndicators(
                activeGesture = activeGestureOrKey,
                volumePercentage = volumeState.volumePercentage,
                maxVolumePercentage = volumeState.maxVolumePercentage,
                brightnessPercentage = brightnessState.brightnessPercentage,
            )
        }
    }

    PlayerErrorDialogs(
        decoderRecoveryState = state.decoderServiceState.recoveryState,
        playbackError = errorState.playbackError,
        hasNextMediaItem = player.hasNextMediaItem(),
        onTryDecoderFallback = { onAction(PlayerAction.TryDecoderFallback) },
        onPlayNextVideo = {
            errorState.dismiss()
            player.seekToNext()
            player.play()
        },
        onExit = {
            errorState.dismiss()
            onAction(PlayerAction.NavigateUp)
        },
    )
}

@OptIn(UnstableApi::class)
@Preview(name = "Landscape", widthDp = 960, heightDp = 540)
@Preview(name = "Portrait", widthDp = 540, heightDp = 960)
@Composable
private fun MediaPlayerContentPreview() {
    NextPlayerTheme(darkTheme = true) {
        MediaPlayerContent(
            player = rememberPreviewPlayer(),
            state = PlayerUiState(),
            onAction = {},
        )
    }
}
