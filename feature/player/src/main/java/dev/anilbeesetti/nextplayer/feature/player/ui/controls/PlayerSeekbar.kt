package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.Chapter
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.LocalUseMaterialYouControls
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun PlayerSeekbar(
    modifier: Modifier = Modifier,
    position: Float,
    duration: Float,
    bufferedPosition: Float = 0f,
    wavingTimeline: Boolean = false,
    isPlaying: Boolean = false,
    chapters: List<Chapter> = emptyList(),
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val safeDuration = duration.coerceAtLeast(1f)
    val safePosition = position.coerceIn(0f, safeDuration)
    val safeBufferedFraction = if (duration > 0f && bufferedPosition > 0f) {
        (bufferedPosition / safeDuration).coerceIn(0f, 1f)
    } else {
        0f
    }

    var isScrubbing by remember { mutableStateOf(false) }

    val handleSeek: (Float) -> Unit = { newValue ->
        isScrubbing = true
        onSeek(newValue)
    }
    val handleSeekFinished: () -> Unit = {
        isScrubbing = false
        onSeekFinished()
    }

    val gapPositions = remember(chapters, duration) {
        if (duration <= 0f) {
            emptyList()
        } else {
            chapters.drop(1)
                .map { (it.startTimeMs.toFloat() / duration).coerceIn(0f, 1f) }
                .filter { it > 0f && it < 1f }
        }
    }

    if (LocalUseMaterialYouControls.current) {
        MaterialYouSlider(
            modifier = modifier.fillMaxWidth(),
            value = safePosition,
            valueRange = 0f..safeDuration,
            bufferedFraction = safeBufferedFraction,
            wavingTimeline = wavingTimeline,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            onValueChange = handleSeek,
            onValueChangeFinished = handleSeekFinished,
            primaryColor = primaryColor,
            gapFractions = gapPositions,
        )
    } else {
        SimpleSlider(
            modifier = modifier.fillMaxWidth(),
            value = safePosition,
            valueRange = 0f..safeDuration,
            bufferedFraction = safeBufferedFraction,
            wavingTimeline = wavingTimeline,
            isPlaying = isPlaying,
            isScrubbing = isScrubbing,
            onValueChange = handleSeek,
            onValueChangeFinished = handleSeekFinished,
            primaryColor = primaryColor,
            gapFractions = gapPositions,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    bufferedFraction: Float = 0f,
    wavingTimeline: Boolean = false,
    isPlaying: Boolean = false,
    isScrubbing: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    gapFractions: List<Float> = emptyList(),
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 16.dp,
) {
    val inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val bufferedTrackColor = Color.White.copy(alpha = 0.85f)
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isUserHolding = isScrubbing || isDragged || isPressed

    val waveAmplitudeFactor by animateFloatAsState(
        targetValue = if (wavingTimeline && isPlaying && !isUserHolding) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "simple_slider_wave_amplitude",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "simple_slider_wave_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "simple_slider_wave_phase",
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier.height(32.dp),
        interactionSource = interactionSource,
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .shadow(4.dp, CircleShape, clip = false)
                    .background(primaryColor, CircleShape),
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (range > 0f) {
                ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
            } else {
                0f
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                val barHeightPx = trackHeight.toPx()
                val centerY = size.height / 2f
                val cornerRadius = CornerRadius(barHeightPx / 2f)
                val activeX = size.width * fraction
                val bufferedX = size.width * bufferedFraction.coerceIn(0f, 1f)

                // Inactive background track
                drawRoundRect(
                    color = inactiveTrackColor,
                    topLeft = Offset(0f, centerY - barHeightPx / 2f),
                    size = Size(size.width, barHeightPx),
                    cornerRadius = cornerRadius,
                )

                // Network buffering track (crisp white bar ahead of playback position)
                if (bufferedX > activeX) {
                    drawRoundRect(
                        color = bufferedTrackColor,
                        topLeft = Offset(activeX, centerY - barHeightPx / 2f),
                        size = Size((bufferedX - activeX).coerceAtLeast(0f), barHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }

                // Active played track (smooth wave when wavingTimeline is enabled, straight line when scrubbing/off)
                if (activeX > 0f) {
                    if (waveAmplitudeFactor > 0.01f) {
                        drawWavingActiveTrack(
                            activeX = activeX,
                            centerY = centerY,
                            strokeWidthPx = barHeightPx,
                            amplitudePx = 2.8.dp.toPx() * waveAmplitudeFactor,
                            wavelengthPx = 48.dp.toPx(),
                            phase = wavePhase,
                            color = primaryColor,
                        )
                    } else {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(0f, centerY - barHeightPx / 2f),
                            size = Size(activeX, barHeightPx),
                            cornerRadius = cornerRadius,
                        )
                    }
                }

                // Cut transparent gaps at chapter boundaries only for normal straight timeline
                // (hide cuts when waving timeline is active so the wave line is seamless and unbroken)
                if (!wavingTimeline) {
                    val gapWidth = 2.dp.toPx()
                    for (gapFraction in gapFractions) {
                        val gapCenter = size.width * gapFraction
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(gapCenter - gapWidth / 2f, 0f),
                            size = Size(gapWidth, size.height),
                            blendMode = BlendMode.Clear,
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialYouSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    bufferedFraction: Float = 0f,
    wavingTimeline: Boolean = false,
    isPlaying: Boolean = false,
    isScrubbing: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    gapFractions: List<Float> = emptyList(),
    trackHeight: Dp = 12.dp,
) {
    val inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val bufferedTrackColor = Color.White.copy(alpha = 0.85f)
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isUserHolding = isScrubbing || isDragged || isPressed

    val waveAmplitudeFactor by animateFloatAsState(
        targetValue = if (wavingTimeline && isPlaying && !isUserHolding) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "m3_slider_wave_amplitude",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "m3_slider_wave_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "m3_slider_wave_phase",
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier,
        interactionSource = interactionSource,
        thumb = {
            Box(
                Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .hoverable(interactionSource = interactionSource)
                    .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
                    .background(primaryColor, CircleShape),
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (range > 0f) {
                ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
            } else {
                0f
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight + 16.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                val barHeightPx = trackHeight.toPx()
                val centerY = size.height / 2f
                val outerRadius = CornerRadius(barHeightPx / 2f)
                val innerRadius = CornerRadius(2.dp.toPx())
                val thumbGap = 8.dp.toPx()
                val activeWidth = (size.width * fraction - thumbGap).coerceAtLeast(0f)
                val inactiveStart = (size.width * fraction + thumbGap).coerceAtMost(size.width)
                val inactiveWidth = (size.width - inactiveStart).coerceAtLeast(0f)
                val bufferedEnd = (size.width * bufferedFraction.coerceIn(0f, 1f)).coerceAtMost(size.width)

                // Active track (left of thumb)
                if (activeWidth > 0f) {
                    if (waveAmplitudeFactor > 0.01f) {
                        drawWavingActiveTrack(
                            activeX = activeWidth,
                            centerY = centerY,
                            strokeWidthPx = (barHeightPx * 0.44f).coerceAtLeast(3.5.dp.toPx()),
                            amplitudePx = 2.8.dp.toPx() * waveAmplitudeFactor,
                            wavelengthPx = 48.dp.toPx(),
                            phase = wavePhase,
                            color = primaryColor,
                        )
                    } else {
                        val path = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    left = 0f,
                                    top = centerY - barHeightPx / 2f,
                                    right = activeWidth,
                                    bottom = centerY + barHeightPx / 2f,
                                    topLeftCornerRadius = outerRadius,
                                    bottomLeftCornerRadius = outerRadius,
                                    topRightCornerRadius = innerRadius,
                                    bottomRightCornerRadius = innerRadius,
                                ),
                            )
                        }
                        drawPath(path, primaryColor)
                    }
                }

                // Inactive track (right of thumb)
                if (inactiveWidth > 0f) {
                    val path = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = inactiveStart,
                                top = centerY - barHeightPx / 2f,
                                right = size.width,
                                bottom = centerY + barHeightPx / 2f,
                                topLeftCornerRadius = innerRadius,
                                bottomLeftCornerRadius = innerRadius,
                                topRightCornerRadius = outerRadius,
                                bottomRightCornerRadius = outerRadius,
                            ),
                        )
                    }
                    drawPath(path, inactiveTrackColor)
                }

                // Network Buffered track (between thumb and bufferedEnd)
                if (bufferedEnd > inactiveStart) {
                    val bufferedWidth = (bufferedEnd - inactiveStart).coerceAtLeast(0f)
                    if (bufferedWidth > 0f) {
                        val path = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    left = inactiveStart,
                                    top = centerY - barHeightPx / 2f,
                                    right = inactiveStart + bufferedWidth,
                                    bottom = centerY + barHeightPx / 2f,
                                    topLeftCornerRadius = innerRadius,
                                    bottomLeftCornerRadius = innerRadius,
                                    topRightCornerRadius = if (bufferedEnd >= size.width - 1f) outerRadius else innerRadius,
                                    bottomRightCornerRadius = if (bufferedEnd >= size.width - 1f) outerRadius else innerRadius,
                                ),
                            )
                        }
                        drawPath(path, bufferedTrackColor)
                    }
                }

                // Cut transparent gaps at chapter boundaries only for normal straight timeline
                // (hide cuts when waving timeline is active so the wave line is seamless and unbroken)
                if (!wavingTimeline) {
                    val gapWidth = 2.dp.toPx()
                    for (gapFraction in gapFractions) {
                        val gapCenter = size.width * gapFraction
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(gapCenter - gapWidth / 2f, 0f),
                            size = Size(gapWidth, size.height),
                            blendMode = BlendMode.Clear,
                        )
                    }
                }
            }
        },
    )
}

private fun DrawScope.drawWavingActiveTrack(
    activeX: Float,
    centerY: Float,
    strokeWidthPx: Float,
    amplitudePx: Float,
    wavelengthPx: Float,
    phase: Float,
    color: Color,
) {
    if (activeX <= 0f) return
    val path = Path()
    val stepPx = 2f
    val taperDistancePx = min(36.dp.toPx(), activeX / 2f).coerceAtLeast(1f)
    var x = 0f
    path.moveTo(0f, centerY)
    while (x < activeX) {
        val nextX = min(x + stepPx, activeX)
        val edgeDistance = min(nextX, activeX - nextX)
        val t = (edgeDistance / taperDistancePx).coerceIn(0f, 1f)
        val envelope = 0.5f * (1f - cos(t * PI.toFloat()))
        val angle = (nextX / wavelengthPx) * (2.0 * PI).toFloat() - phase
        val y = centerY + sin(angle) * amplitudePx * envelope
        path.lineTo(nextX, y)
        x = nextX
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

@UnstableApi
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerSeekbarPreview() {
    NextPlayerTheme {
        PlayerSeekbar(
            position = 30000f,
            duration = 120000f,
            bufferedPosition = 75000f,
            wavingTimeline = true,
            isPlaying = true,
            chapters = emptyList(),
            onSeek = {},
            onSeekFinished = {},
        )
    }
}
