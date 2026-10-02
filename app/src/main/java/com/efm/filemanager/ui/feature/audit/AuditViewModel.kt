package com.efm.filemanager.ui.feature.audit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.audit.AuditEventEntity
import com.efm.filemanager.data.audit.AuditHashChain
import com.efm.filemanager.data.audit.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class AuditViewModel
    @Inject
    constructor(
        private val auditRepository: AuditRepository,
    ) : ViewModel() {
        private val allEntries = auditRepository.observeAll()
        private val _filter = MutableStateFlow(AuditFilter.ALL)
        val filter: StateFlow<AuditFilter> = _filter.asStateFlow()
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        val entries: StateFlow<List<AuditEventEntity>> =
            combine(allEntries, _filter, _searchQuery) { all, filter, query ->
                val byFilter = if (filter == AuditFilter.FAILED_ONLY) all.filter { !it.success } else all
                if (query.isBlank()) byFilter else byFilter.filter { it.targetName.contains(query, ignoreCase = true) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        /** Independent of [filter], so the filter menu doesn't disappear just because it happens to match nothing right now. */
        val hasEntries: StateFlow<Boolean> =
            allEntries.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

        /** Non-null once the hash chain (see [AuditHashChain]) finds a tampered or missing entry -- surfaced as a warning banner. */
        val tamperedEntry: StateFlow<AuditEventEntity?> =
            allEntries
                .map { AuditHashChain.findFirstBrokenLink(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        fun setFilter(filter: AuditFilter) {
            _filter.value = filter
        }

        fun setSearchQuery(query: String) {
            _searchQuery.value = query
        }

        /** [text] is pre-built by the caller, which has the `Context`/`stringResource` access needed to localize action labels. */
        fun exportTo(
            uri: Uri,
            text: String,
        ) {
            viewModelScope.launch { auditRepository.exportText(uri, text) }
        }
    }
