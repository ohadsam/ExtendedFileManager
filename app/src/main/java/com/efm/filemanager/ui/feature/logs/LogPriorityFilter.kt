package com.efm.filemanager.ui.feature.logs

import android.util.Log
import com.efm.filemanager.R

enum class LogPriorityFilter(val minPriority: Int, val labelRes: Int) {
    ALL(Int.MIN_VALUE, R.string.logs_filter_all),
    INFO(Log.INFO, R.string.logs_filter_info),
    WARN(Log.WARN, R.string.logs_filter_warn),
    ERROR(Log.ERROR, R.string.logs_filter_error),
}
