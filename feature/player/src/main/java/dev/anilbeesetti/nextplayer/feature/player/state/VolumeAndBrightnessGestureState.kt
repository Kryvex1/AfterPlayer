package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntSize
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberVolumeAndBrightnessGestureState(
    volumeState: VolumeState,
    brightnessState: BrightnessState,
    enableVolumeGesture: Boolean,
    enableBrightnessGesture: Boolean,
    volumeGestureSensitivity: Float,
    brightnessGestureSensitivity: Float,
    onMinimize: () -> Unit = {},
): VolumeAndBrightnessGestureState {
    val coroutineScope = rememberCoroutineScope()
    val volumeAndBrightnessGestureState = remember(volumeState, brightnessState, onMinimize) {
        VolumeAndBrightnessGestureState(
            volumeState = volumeState,
            brightnessState = brightnessState,
            enableVolumeGesture = enableVolumeGesture,
            enableBrightnessGesture = enableBrightnessGesture,
            volumeGestureSensitivity = volumeGestureSensitivity,
            brightnessGestureSensitivity = brightnessGestureSensitivity,
            onMinimize = onMinimize,
            coroutineScope = coroutineScope,
        )
    }
    return volumeAndBrightnessGestureState
}

@Stable
class VolumeAndBrightnessGestureState(
    private val volumeState: VolumeState,
    private val brightnessState: BrightnessState,
    private val enableVolumeGesture: Boolean = true,
    private val enableBrightnessGesture: Boolean = true,
    private val volumeGestureSensitivity: Float,
    private val brightnessGestureSensitivity: Float,
    private val onMinimize: () -> Unit = {},
    private val coroutineScope: CoroutineScope,
) {
    var activeGesture: VerticalGesture? by mutableStateOf(null)
        private set

    var volumeChangePercentage: Int by mutableIntStateOf(0)
        private set

    var brightnessChangePercentage: Int by mutableIntStateOf(0)
        private set

    private var startingY = 0f
    private var startVolumePercentage = 0
    private var startBrightnessPercentage = 0
    private var hasTriggeredMinimize = false
    private var job: Job? = null

    fun onDragStart(offset: Offset, size: IntSize) {
        val leftBoundary = size.width * 0.35f
        val rightBoundary = size.width * 0.65f
        job?.cancel()
        activeGesture = when {
            offset.x < leftBoundary -> VerticalGesture.BRIGHTNESS.takeIf { enableBrightnessGesture }
            offset.x > rightBoundary -> VerticalGesture.VOLUME.takeIf { enableVolumeGesture }
            else -> VerticalGesture.MINIMIZE
        }
        startingY = offset.y
        hasTriggeredMinimize = false
        volumeState.syncWithSystem()
        startVolumePercentage = volumeState.volumePercentage
        startBrightnessPercentage = brightnessState.brightnessPercentage
    }

    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val activeGesture = activeGesture ?: return
        if (change.isConsumed) return

        when (activeGesture) {
            VerticalGesture.VOLUME -> {
                val maxVolumePercentage = volumeState.maxVolumePercentage
                val volumeChange = (startingY - change.position.y) * (volumeGestureSensitivity / 10)
                val newVolume = startVolumePercentage + volumeChange.toInt()
                volumeChangePercentage = (newVolume - startVolumePercentage).coerceIn(
                    minimumValue = 0 - startVolumePercentage,
                    maximumValue = maxVolumePercentage - startVolumePercentage,
                )
                brightnessChangePercentage = 0
                volumeState.updateVolumePercentage(newVolume)
            }

            VerticalGesture.BRIGHTNESS -> {
                val brightnessChange = (startingY - change.position.y) * (brightnessGestureSensitivity / 10)
                val newBrightness = startBrightnessPercentage + brightnessChange.toInt()
                brightnessChangePercentage = (newBrightness - startBrightnessPercentage).coerceIn(
                    minimumValue = 0 - startBrightnessPercentage,
                    maximumValue = MAX_BRIGHTNESS_PERCENTAGE - startBrightnessPercentage,
                )
                volumeChangePercentage = 0
                brightnessState.updateBrightnessPercentage(newBrightness)
            }

            VerticalGesture.MINIMIZE -> {
                val dragY = change.position.y - startingY
                if (dragY > 120f && !hasTriggeredMinimize) {
                    hasTriggeredMinimize = true
                    change.consume()
                    onMinimize()
                }
            }
        }
    }

    fun onDragEnd() {
        startingY = 0f
        startVolumePercentage = 0
        startBrightnessPercentage = 0
        hasTriggeredMinimize = false

        job?.cancel()
        job = coroutineScope.launch {
            delay(1.seconds)
            activeGesture = null
            volumeChangePercentage = 0
            brightnessChangePercentage = 0
        }
    }

    companion object {
        private const val MAX_BRIGHTNESS_PERCENTAGE = 100
    }
}

enum class VerticalGesture {
    VOLUME,
    BRIGHTNESS,
    MINIMIZE,
}
