package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.ui.R as CoreUiR
import dev.anilbeesetti.nextplayer.feature.player.extensions.toTypeface
import dev.anilbeesetti.nextplayer.feature.player.state.rememberCuesState

@OptIn(UnstableApi::class)
@Composable
fun SubtitleView(
    modifier: Modifier = Modifier,
    player: Player,
    isInPictureInPictureMode: Boolean,
    configuration: SubtitleConfiguration,
) {
    val cuesState = rememberCuesState(player)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            SubtitleView(context).apply {
                setBottomPaddingFraction(0.04f)
                applySubtitleStyle(
                    context = context,
                    subtitleView = this,
                    cues = cuesState.currentCues?.cues,
                    configuration = configuration,
                )
            }
        },
        update = { subtitleView ->
            val cues = cuesState.currentCues?.cues
            subtitleView.setCues(cues)
            subtitleView.setBottomPaddingFraction(0.04f)
            applySubtitleStyle(
                context = subtitleView.context,
                subtitleView = subtitleView,
                cues = cues,
                configuration = configuration,
            )
            if (isInPictureInPictureMode) {
                subtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 0.75f)
            } else {
                val isPortrait = subtitleView.context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                val targetSize = if (isPortrait) {
                    (configuration.textSize.toFloat() * 0.85f).coerceAtLeast(8f)
                } else {
                    configuration.textSize.toFloat()
                }
                subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, targetSize)
            }
        },
    )
}

@OptIn(UnstableApi::class)
private fun applySubtitleStyle(
    context: Context,
    subtitleView: SubtitleView,
    cues: List<Cue>?,
    configuration: SubtitleConfiguration,
) {
    val captioningManager = getSystemService(context, CaptioningManager::class.java)
    if (configuration.useSystemCaptionStyle && captioningManager != null) {
        val systemCaptionStyle = CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        subtitleView.setStyle(systemCaptionStyle)
    } else {
        val hasHindiText = cues?.any { it.text.containsDevanagari() } == true
        val usePoppins = hasHindiText || configuration.font == Font.POPPINS
        val finalTypeface = if (usePoppins) {
            val poppins = ResourcesCompat.getFont(context, CoreUiR.font.poppins) ?: Typeface.SANS_SERIF
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                Typeface.create(poppins, if (configuration.textBold) 700 else 400, false)
            } else {
                Typeface.create(poppins, if (configuration.textBold) Typeface.BOLD else Typeface.NORMAL)
            }
        } else {
            val inter = ResourcesCompat.getFont(context, CoreUiR.font.inter) ?: Typeface.SANS_SERIF
            val baseTypeface = when (configuration.font) {
                Font.DEFAULT -> inter
                else -> configuration.font.toTypeface()
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && configuration.font == Font.DEFAULT) {
                Typeface.create(baseTypeface, if (configuration.textBold) 700 else 400, false)
            } else {
                Typeface.create(baseTypeface, if (configuration.textBold) Typeface.BOLD else Typeface.NORMAL)
            }
        }

        val userStyle = CaptionStyleCompat(
            android.graphics.Color.WHITE,
            android.graphics.Color.BLACK.takeIf { configuration.showBackground } ?: android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            android.graphics.Color.BLACK,
            finalTypeface,
        )
        subtitleView.setStyle(userStyle)
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val targetSize = if (isPortrait) {
            (configuration.textSize.toFloat() * 0.85f).coerceAtLeast(8f)
        } else {
            configuration.textSize.toFloat()
        }
        subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, targetSize)
    }
    subtitleView.setApplyEmbeddedStyles(configuration.applyEmbeddedStyles)
}

private fun CharSequence?.containsDevanagari(): Boolean {
    if (this == null) return false
    for (i in 0 until length) {
        val code = this[i].code
        if (code in 0x0900..0x097F || code in 0xA8E0..0xA8FF || code in 0x1CD0..0x1CFF) return true
    }
    return false
}

@Stable
data class SubtitleConfiguration(
    val useSystemCaptionStyle: Boolean,
    val showBackground: Boolean,
    val font: Font,
    val textSize: Int,
    val textBold: Boolean,
    val applyEmbeddedStyles: Boolean,
)
