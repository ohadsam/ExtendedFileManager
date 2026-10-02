package com.efm.filemanager.ui.feature.audit

import com.efm.filemanager.R

enum class AuditFilter(val labelRes: Int) {
    ALL(R.string.audit_filter_all),
    FAILED_ONLY(R.string.audit_filter_failed_only),
}
