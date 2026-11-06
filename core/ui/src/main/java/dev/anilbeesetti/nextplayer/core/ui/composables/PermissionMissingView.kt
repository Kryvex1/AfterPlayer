package dev.anilbeesetti.nextplayer.core.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun PermissionMissingView(
    isGranted: Boolean,
    showRationale: Boolean,
    permission: String,
    launchPermissionRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isGranted) {
        content()
    } else {
        PermissionDetailView(
            title = stringResource(id = R.string.storage_permission_required),
            text = stringResource(id = R.string.storage_permission_description),
            onRequestPermission = launchPermissionRequest,
        )
    }
}
