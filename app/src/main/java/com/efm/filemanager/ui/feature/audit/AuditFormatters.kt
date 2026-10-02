package com.efm.filemanager.ui.feature.audit

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatAuditTimestamp(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale.getDefault()).withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
