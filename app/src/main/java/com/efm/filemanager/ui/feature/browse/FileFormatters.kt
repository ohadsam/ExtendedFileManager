package com.efm.filemanager.ui.feature.browse

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sizeUnits = listOf("KB", "MB", "GB", "TB")

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024 && unitIndex < sizeUnits.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, sizeUnits[unitIndex])
}

fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
