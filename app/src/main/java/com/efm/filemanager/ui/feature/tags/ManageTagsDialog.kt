package com.efm.filemanager.ui.feature.tags

import com.efm.filemanager.domain.model.FileTag

internal sealed interface ManageTagsDialog {
    data object Create : ManageTagsDialog

    data class Edit(val tag: FileTag) : ManageTagsDialog

    data class Merge(val tag: FileTag) : ManageTagsDialog

    data class Delete(val tag: FileTag) : ManageTagsDialog
}
