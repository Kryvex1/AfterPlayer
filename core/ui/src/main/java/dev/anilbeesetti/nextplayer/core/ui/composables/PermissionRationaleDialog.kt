package dev.anilbeesetti.nextplayer.core.ui.composables

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.preview.DayNightPreview
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

@Composable
fun PermissionRationaleDialog(
    text: String = stringResource(R.string.storage_permission_description),
    modifier: Modifier = Modifier,
    onConfirmButtonClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        icon = {
            Icon(
                imageVector = NextIcons.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.permission_request),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmButtonClick,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.allow_access))
            }
        },
    )
}

@DayNightPreview
@Composable
fun PermissionRationaleDialogPreview() {
    NextPlayerTheme {
        Surface {
            PermissionRationaleDialog(
                text = stringResource(id = R.string.storage_permission_description),
                onConfirmButtonClick = {},
            )
        }
    }
}
