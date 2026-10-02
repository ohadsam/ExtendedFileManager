package com.efm.filemanager.ui.feature.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.audit.AuditEventEntity
import com.efm.filemanager.data.audit.AuditHashChain
import com.efm.filemanager.data.audit.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class AuditViewModel
    @Inject
    constructor(
        auditRepository: AuditRepository,
    ) : ViewModel() {
        private val allEntries = auditRepository.observeAll()

        val entries: StateFlow<List<AuditEventEntity>> =
            allEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        /** Non-null once the hash chain (see [AuditHashChain]) finds a tampered or missing entry -- surfaced as a warning banner. */
        val tamperedEntry: StateFlow<AuditEventEntity?> =
            allEntries
                .map { AuditHashChain.findFirstBrokenLink(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)
    }
