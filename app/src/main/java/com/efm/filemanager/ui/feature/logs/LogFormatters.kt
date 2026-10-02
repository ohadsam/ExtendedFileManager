package com.efm.filemanager.ui.feature.logs

import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatLogTimestamp(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale.getDefault()).withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}

/** A short, all-caps label for a `Timber`/`Log` priority int, for display next to a log row. */
fun logPriorityLabel(priority: Int): String =
    when (priority) {
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> "LOG"
    }
