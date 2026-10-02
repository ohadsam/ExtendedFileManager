package com.efm.filemanager.ui.feature.audit

import com.efm.filemanager.R
import com.efm.filemanager.data.audit.AuditAction

internal fun AuditAction.labelRes(): Int =
    when (this) {
        AuditAction.CREATE_FOLDER -> R.string.audit_action_create_folder
        AuditAction.CREATE_FILE -> R.string.audit_action_create_file
        AuditAction.RENAME -> R.string.audit_action_rename
        AuditAction.DELETE -> R.string.audit_action_delete
        AuditAction.RESTORE -> R.string.audit_action_restore
        AuditAction.MOVE -> R.string.audit_action_move
        AuditAction.COPY -> R.string.audit_action_copy
        AuditAction.ADD_TO_VAULT -> R.string.audit_action_add_to_vault
        AuditAction.EXPORT_FROM_VAULT -> R.string.audit_action_export_from_vault
        AuditAction.REMOVE_FROM_VAULT -> R.string.audit_action_remove_from_vault
    }
