package com.efm.filemanager.domain.model

/**
 * How [com.efm.filemanager.data.statistics.StatisticsRepository.filesByCategory] orders its
 * result -- SIZE for the by-type/largest-files widgets' own drill-down, RECENT for the
 * recently-modified widget's.
 */
enum class GlobalFilesSort { SIZE, RECENT }
