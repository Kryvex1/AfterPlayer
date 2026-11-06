package dev.anilbeesetti.nextplayer.settings.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.components.CancelButton
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus

@Composable
fun OptionsDialog(
    text: String,
    onDismissClick: () -> Unit,
    options: LazyListScope.() -> Unit,
) {
    NextDialog(
        onDismissRequest = onDismissClick,
        title = {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        content = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .selectableGroup()
                    .tvListFocus(),
                content = options,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        confirmButton = { CancelButton(onClick = onDismissClick) },
        dismissButton = null,
    )
}
