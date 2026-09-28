package com.efm.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
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
import com.efm.filemanager.data.archive.ConflictPolicy

@Composable
fun ConflictPolicyDialog(
    showReplaceWarning: Boolean,
    onConfirm: (ConflictPolicy) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(ConflictPolicy.RENAME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conflict_policy_title)) },
        text = {
            Column {
                if (showReplaceWarning) {
                    Text(
                        text = stringResource(R.string.conflict_policy_replace_warning),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                ConflictPolicyRow(ConflictPolicy.RENAME, selected, stringResource(R.string.conflict_policy_rename)) {
                    selected = it
                }
                ConflictPolicyRow(ConflictPolicy.OVERWRITE, selected, stringResource(R.string.conflict_policy_overwrite)) {
                    selected = it
                }
                ConflictPolicyRow(ConflictPolicy.SKIP, selected, stringResource(R.string.conflict_policy_skip)) {
                    selected = it
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.conflict_policy_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ConflictPolicyRow(
    value: ConflictPolicy,
    selected: ConflictPolicy,
    label: String,
    onSelect: (ConflictPolicy) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect(value) }
                .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = value == selected, onClick = { onSelect(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
