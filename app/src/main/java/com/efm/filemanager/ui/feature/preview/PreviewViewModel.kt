package com.efm.filemanager.ui.feature.preview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel
    @Inject
    constructor(
        private val previewSessionHolder: PreviewSessionHolder,
    ) : ViewModel() {
        val session: StateFlow<PreviewSession?> = previewSessionHolder.session

        override fun onCleared() {
            previewSessionHolder.clear()
        }
    }
