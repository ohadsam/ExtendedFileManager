package com.efm.filemanager.data.audit

enum class AuditAction {
    CREATE_FOLDER,
    CREATE_FILE,
    RENAME,
    DELETE,
    RESTORE,
    MOVE,
    COPY,
    ADD_TO_VAULT,
    EXPORT_FROM_VAULT,
    REMOVE_FROM_VAULT,
}
