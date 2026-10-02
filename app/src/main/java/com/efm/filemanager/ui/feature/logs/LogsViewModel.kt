package com.efm.filemanager.ui.feature.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.local.LogEntryEntity
import com.efm.filemanager.data.logs.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class LogsViewModel
    @Inject
    constructor(
        logRepository: LogRepository,
    ) : ViewModel() {
        val entries: StateFlow<List<LogEntryEntity>> =
            logRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    }
