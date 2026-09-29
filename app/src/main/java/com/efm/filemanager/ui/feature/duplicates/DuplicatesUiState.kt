package com.efm.filemanager.ui.feature.duplicates

import com.efm.filemanager.data.duplicates.ScanProgress
import com.efm.filemanager.domain.model.DuplicateGroup

enum class ScanRunState { IDLE, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class DuplicatesUiState(
    val runState: ScanRunState = ScanRunState.IDLE,
    val progress: ScanProgress? = null,
    val groups: List<DuplicateGroup> = emptyList(),
)
