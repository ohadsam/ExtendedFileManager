package com.efm.filemanager.data.documenttree

/** Thrown by [FileOperationsRepository.delete] to refuse a locked item -- see docs/PLAN.md Phase 9. */
class LockedFileException(fileName: String) : Exception("$fileName is locked")
