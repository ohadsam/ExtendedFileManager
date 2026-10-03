package com.efm.filemanager.ui.feature.browse

import android.net.Uri

/** [BrowsePickerController]'s callbacks into the owning ViewModel, bundled to keep its own constructor arity down. */
data class BrowsePickerCallbacks(
    val currentSourceParentUri: () -> Uri?,
    val currentRootCrumb: () -> BreadcrumbEntry?,
    val onOperationFailed: suspend () -> Unit,
)
