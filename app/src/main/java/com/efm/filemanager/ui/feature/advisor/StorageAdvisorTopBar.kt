package com.efm.filemanager.ui.feature.advisor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.efm.filemanager.R

@Composable
internal fun StorageAdvisorTopBar(
    onOpenDrawer: () -> Unit,
    runState: AdvisorScanRunState,
    onScan: () -> Unit,
    onCancel: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_drawer_open))
            }
        },
        title = { Text(stringResource(R.string.nav_insights)) },
        actions = {
            if (runState == AdvisorScanRunState.RUNNING) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.storage_advisor_cancel_scan))
                }
            } else {
                IconButton(onClick = onScan) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.storage_advisor_scan))
                }
            }
        },
    )
}
