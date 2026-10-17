package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.ui.components.requestFocusUntilLanded
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val layoutDirection = LocalLayoutDirection.current
    val endPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateEndPadding(layoutDirection)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(show) {
        if (show && isTv) {
            focusRequester.requestFocusUntilLanded(attempts = 5)
        }
    }

    AnimatedVisibility(
        modifier = Modifier.align(
            if (configuration.isPortrait) {
                Alignment.BottomCenter
            } else {
                Alignment.CenterEnd
            },
        ),
        visible = show,
        enter = (if (configuration.isPortrait) {
            slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it }
        } else {
            slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it }
        }) + fadeIn(animationSpec = tween(250)),
        exit = (if (configuration.isPortrait) {
            slideOutVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it }
        } else {
            slideOutHorizontally(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it }
        }) + fadeOut(animationSpec = tween(200)),
    ) {
        if (configuration.isPortrait) {
            // Portrait: Bottom Sheet
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f),
            ) {
                Column(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusGroup()
                        .padding(end = endPadding),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = NextIcons.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    content()
                }
            }
        } else {
            // Landscape: Clean Side Drawer
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = modifier
                    .fillMaxWidth(0.44f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusGroup()
                        .padding(top = 16.dp)
                        .padding(end = endPadding),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = NextIcons.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    content()
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOverlayView() {
    NextPlayerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OverlayView(modifier = Modifier.align(Alignment.BottomCenter), title = "Selector view", show = true) {
                Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum")
            }
        }
    }
}
