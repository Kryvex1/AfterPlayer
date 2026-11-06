package dev.anilbeesetti.nextplayer.core.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
fun NextDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    dialogProperties: DialogProperties = NextDialogDefaults.dialogProperties,
) {
    val configuration = LocalConfiguration.current
    val maxDialogWidth = minOf(360.dp, configuration.screenWidthDp.dp - NextDialogDefaults.dialogMargin * 2)

    AlertDialog(
        title = title,
        text = {
            val view = LocalView.current
            DisposableEffect(view) {
                var parent = view.parent
                while (parent != null && parent !is DialogWindowProvider) {
                    parent = parent.parent
                }
                (parent as? DialogWindowProvider)?.window?.let { window ->
                    try {
                        window.setLayout(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                        )
                        window.setDimAmount(0.60f)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                            window.attributes = window.attributes.apply {
                                blurBehindRadius = 36
                            }
                        }
                        window.setWindowAnimations(android.R.style.Animation_Dialog)
                    } catch (_: Exception) {
                    }
                }
                onDispose { }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                content()
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .widthIn(min = 280.dp, max = maxDialogWidth)
            .wrapContentHeight(),
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        properties = dialogProperties,
    )
}

@Composable
fun NextDialogWithDoneAndCancelButtons(
    title: String,
    onDoneClick: () -> Unit,
    onDismissClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    NextDialog(
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        confirmButton = { DoneButton(onClick = onDoneClick) },
        dismissButton = { CancelButton(onClick = onDismissClick) },
        onDismissRequest = onDismissClick,
        content = content,
    )
}

object NextDialogDefaults {
    val dialogProperties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = true,
    )
    val dialogMargin: Dp = 24.dp
}
