package com.efm.filemanager.data.metadata

import com.efm.filemanager.domain.model.FavoriteCollection
import com.efm.filemanager.domain.model.FileTag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Bundles the tag catalog, favorite collections, and selection quick-actions every screen's
 * ViewModel needs (Browse, Search, Duplicates, Preview -- docs/PLAN.md Phase 9) behind one
 * constructor parameter instead of three, keeping each ViewModel's own parameter count down.
 */
class ScreenMetadataSupport
    @Inject
    constructor(
        private val tagRepository: TagRepository,
        private val favoriteRepository: FavoriteRepository,
        val metadataActions: SelectionMetadataActions,
    ) {
        val tags: Flow<List<FileTag>> = tagRepository.tags
        val favoriteCollections: Flow<List<FavoriteCollection>> = favoriteRepository.collections
    }
