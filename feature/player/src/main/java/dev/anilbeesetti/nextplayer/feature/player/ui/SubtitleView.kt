package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView as Media3SubtitleView
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.SubtitleAnimation
import dev.anilbeesetti.nextplayer.core.ui.R as CoreUiR
import dev.anilbeesetti.nextplayer.core.ui.theme.CustomFontConfig
import dev.anilbeesetti.nextplayer.core.ui.theme.PoppinsFontFamily
import dev.anilbeesetti.nextplayer.core.ui.theme.SFProFontFamily
import dev.anilbeesetti.nextplayer.feature.player.extensions.toTypeface
import dev.anilbeesetti.nextplayer.feature.player.state.rememberCuesState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun SubtitleView(
    modifier: Modifier = Modifier,
    player: Player,
    isInPictureInPictureMode: Boolean,
    configuration: SubtitleConfiguration,
) {
    val cuesState = rememberCuesState(player)
    val cues = cuesState.currentCues?.cues

    if (configuration.useSystemCaptionStyle) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                Media3SubtitleView(context).apply {
                    setBottomPaddingFraction(configuration.bottomPadding.toFloat() / 400f)
                    applySystemSubtitleStyle(
                        context = context,
                        subtitleView = this,
                        configuration = configuration,
                    )
                }
            },
            update = { subtitleView ->
                subtitleView.setCues(cues)
                subtitleView.setBottomPaddingFraction(configuration.bottomPadding.toFloat() / 400f)
                applySystemSubtitleStyle(
                    context = subtitleView.context,
                    subtitleView = subtitleView,
                    configuration = configuration,
                )
                if (isInPictureInPictureMode) {
                    subtitleView.setFractionalTextSize(Media3SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 0.75f)
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
    } else {
        ComposeSubtitleRenderer(
            modifier = modifier,
            cues = cues,
            isInPictureInPictureMode = isInPictureInPictureMode,
            configuration = configuration,
        )
    }
}

@Composable
private fun ComposeSubtitleRenderer(
    modifier: Modifier = Modifier,
    cues: List<Cue>?,
    isInPictureInPictureMode: Boolean,
    configuration: SubtitleConfiguration,
) {
    val context = LocalContext.current
    val screenConfig = LocalConfiguration.current
    val isPortrait = screenConfig.orientation == Configuration.ORIENTATION_PORTRAIT

    val rawText = remember(cues) {
        cues?.joinToString("\n") { it.text?.toString().orEmpty() }?.trim().orEmpty()
    }

    if (rawText.isBlank()) return

    val targetSize = remember(configuration.textSize, isPortrait, isInPictureInPictureMode) {
        when {
            isInPictureInPictureMode -> (configuration.textSize.toFloat() * 0.65f).coerceAtLeast(8f)
            isPortrait -> (configuration.textSize.toFloat() * 0.85f).coerceAtLeast(8f)
            else -> configuration.textSize.toFloat()
        }
    }

    val containsDevanagari = remember(rawText) { rawText.containsDevanagari() }
    val resolvedFontFamily = remember(
        configuration.font,
        configuration.customRegularFontUri,
        configuration.customBoldFontUri,
        configuration.customHindiFontUri,
        containsDevanagari,
    ) {
        if (containsDevanagari || configuration.font == Font.POPPINS) {
            CustomFontConfig.resolveHindiFontFamily(context, configuration.customHindiFontUri)
        } else if (configuration.font == Font.DEFAULT) {
            CustomFontConfig(
                regularFontUri = configuration.customRegularFontUri,
                boldFontUri = configuration.customBoldFontUri,
            ).resolveFontFamily(context)
        } else {
            when (configuration.font) {
                Font.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
                Font.SANS_SERIF -> androidx.compose.ui.text.font.FontFamily.SansSerif
                Font.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
                else -> SFProFontFamily
            }
        }
    }

    var typewriterLength by remember(rawText, configuration.animation) { mutableIntStateOf(0) }
    LaunchedEffect(rawText, configuration.animation) {
        if (configuration.animation == SubtitleAnimation.TYPEWRITER) {
            typewriterLength = 0
            val totalChars = rawText.length
            val stepDelay = (400L / totalChars.coerceAtLeast(1)).coerceIn(15L, 50L)
            for (i in 1..totalChars) {
                typewriterLength = i
                delay(stepDelay)
            }
        } else {
            typewriterLength = rawText.length
        }
    }

    val displayText = if (configuration.animation == SubtitleAnimation.TYPEWRITER) {
        rawText.take(typewriterLength)
    } else {
        rawText
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = configuration.bottomPadding.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (configuration.animation == SubtitleAnimation.FADE) {
            AnimatedContent(
                targetState = displayText,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "subtitle_fade",
            ) { text ->
                SubtitlePill(
                    text = text,
                    fontFamily = resolvedFontFamily,
                    fontSize = targetSize,
                    isBold = configuration.textBold,
                    showBackground = configuration.showBackground,
                )
            }
        } else {
            SubtitlePill(
                text = displayText,
                fontFamily = resolvedFontFamily,
                fontSize = targetSize,
                isBold = configuration.textBold,
                showBackground = configuration.showBackground,
            )
        }
    }
}

@Composable
private fun SubtitlePill(
    text: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: Float,
    isBold: Boolean,
    showBackground: Boolean,
) {
    Surface(
        color = if (showBackground) Color.Black.copy(alpha = 0.72f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = fontSize.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f,
                ),
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@OptIn(UnstableApi::class)
private fun applySystemSubtitleStyle(
    context: Context,
    subtitleView: Media3SubtitleView,
    configuration: SubtitleConfiguration,
) {
    val captioningManager = getSystemService(context, CaptioningManager::class.java)
    if (captioningManager != null) {
        val systemCaptionStyle = CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
        subtitleView.setStyle(systemCaptionStyle)
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
    val customRegularFontUri: String? = null,
    val customBoldFontUri: String? = null,
    val customHindiFontUri: String? = null,
    val animation: SubtitleAnimation = SubtitleAnimation.NONE,
    val bottomPadding: Int = 16,
)
