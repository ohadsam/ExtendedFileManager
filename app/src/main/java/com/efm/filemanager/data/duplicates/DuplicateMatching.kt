package com.efm.filemanager.data.duplicates

import com.efm.filemanager.data.local.FileEntryEntity

/** Files sharing the same size -- the cheap first-pass filter before any hashing happens. */
internal fun candidateSizeGroups(files: List<FileEntryEntity>): List<List<FileEntryEntity>> =
    files.groupBy { entry -> entry.size }.values.filter { group -> group.size >= 2 }

/** Groups already-hashed candidates by their hash, keeping only groups of 2+ (an actual duplicate). */
internal fun groupDuplicateCandidates(hashed: List<Pair<FileEntryEntity, String>>): List<Pair<String, List<FileEntryEntity>>> =
    hashed.groupBy({ pair -> pair.second }, { pair -> pair.first }).filter { (_, group) -> group.size >= 2 }.toList()
