package com.efm.filemanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.efm.filemanager.R

/**
 * A small (i) affordance for a short contextual explanation, with an optional link
 * into the full user guide for more detail. Used from Phase 3 onward on screens
 * non-obvious enough to warrant one -- not on every screen reflexively.
 */
@Composable
fun InfoButton(
    title: String,
    description: String,
    onLearnMore: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showDialog = true }) {
        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info_button_content_description))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    Text(description)
                    if (onLearnMore != null) {
                        TextButton(
                            onClick = {
                                showDialog = false
                                onLearnMore()
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.learn_more))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}
