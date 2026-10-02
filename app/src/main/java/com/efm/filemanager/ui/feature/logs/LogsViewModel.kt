package com.efm.filemanager.ui.feature.logs

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.local.LogEntryEntity
import com.efm.filemanager.data.logs.LogRepository
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
class LogsViewModel
    @Inject
    constructor(
        private val logRepository: LogRepository,
    ) : ViewModel() {
        private val _priorityFilter = MutableStateFlow(LogPriorityFilter.ALL)
        val priorityFilter: StateFlow<LogPriorityFilter> = _priorityFilter.asStateFlow()

        private val allEntries = logRepository.observeAll()

        val entries: StateFlow<List<LogEntryEntity>> =
            combine(allEntries, _priorityFilter) { all, filter ->
                all.filter { it.priority >= filter.minPriority }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        // Independent of the current filter, so the filter/clear actions don't disappear just
        // because the active filter happens to match nothing right now.
        val hasEntries: StateFlow<Boolean> =
            allEntries.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

        fun setPriorityFilter(filter: LogPriorityFilter) {
            _priorityFilter.value = filter
        }

        fun clearAll() {
            viewModelScope.launch { logRepository.clearAll() }
        }

        fun exportTo(uri: Uri) {
            val text = buildExportText(entries.value)
            viewModelScope.launch { logRepository.exportText(uri, text) }
        }
    }

private fun buildExportText(entries: List<LogEntryEntity>): String =
    entries.joinToString(separator = "\n") { entry ->
        val header =
            "${formatLogTimestamp(entry.timestamp)} ${logPriorityLabel(entry.priority)}" +
                (entry.tag?.let { " $it" } ?: "")
        val stackTrace = entry.stackTrace?.let { "\n$it" } ?: ""
        "$header: ${entry.message}$stackTrace"
    }
