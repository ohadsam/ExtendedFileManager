package com.efm.filemanager.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.efm.filemanager.domain.model.AppearanceMode
import com.efm.filemanager.domain.model.ViewMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "efm_preferences")

private const val DEFAULT_ADVISOR_MIN_SIZE_MB = 100
private const val DEFAULT_ADVISOR_UNUSED_MONTHS = 6

/**
 * Shared DataStore-backed preferences store -- later phases (per-folder settings, ...)
 * extend this instead of each adding their own DataStore.
 */
class PreferencesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val appearanceMode: Flow<AppearanceMode> =
            context.dataStore.data.map { prefs -> appearanceModeFromStoredValue(prefs[APPEARANCE_MODE_KEY]) }

        val dynamicColorEnabled: Flow<Boolean> =
            context.dataStore.data.map { prefs -> prefs[DYNAMIC_COLOR_KEY] ?: true }

        val viewMode: Flow<ViewMode> =
            context.dataStore.data.map { prefs -> viewModeFromStoredValue(prefs[VIEW_MODE_KEY]) }

        /** Phase 10's "large and unused" thresholds, in the user-facing units Settings shows (MB, months). */
        val advisorMinSizeMb: Flow<Int> =
            context.dataStore.data.map { prefs -> prefs[ADVISOR_MIN_SIZE_MB_KEY] ?: DEFAULT_ADVISOR_MIN_SIZE_MB }

        val advisorUnusedMonths: Flow<Int> =
            context.dataStore.data.map { prefs -> prefs[ADVISOR_UNUSED_MONTHS_KEY] ?: DEFAULT_ADVISOR_UNUSED_MONTHS }

        /** Null means "never recorded" (a fresh install), distinct from any real version code, all of which are >= 1. */
        val lastSeenWhatsNewVersion: Flow<Int?> =
            context.dataStore.data.map { prefs -> prefs[LAST_SEEN_WHATS_NEW_VERSION_KEY] }

        /**
         * Phase 17's cheap "has the file index changed since I last looked" signal: bumped by
         * [bumpChangeVersion] every time [com.efm.filemanager.data.documenttree.DocumentTreeRepository]
         * re-caches a folder's contents, so a feature can compare a stored value against this one
         * instead of re-scanning just to find out whether anything moved.
         */
        val changeVersion: Flow<Long> =
            context.dataStore.data.map { prefs -> prefs[CHANGE_VERSION_KEY] ?: 0L }

        suspend fun setAppearanceMode(mode: AppearanceMode) {
            context.dataStore.edit { prefs -> prefs[APPEARANCE_MODE_KEY] = mode.name }
        }

        suspend fun setDynamicColorEnabled(enabled: Boolean) {
            context.dataStore.edit { prefs -> prefs[DYNAMIC_COLOR_KEY] = enabled }
        }

        suspend fun setViewMode(mode: ViewMode) {
            context.dataStore.edit { prefs -> prefs[VIEW_MODE_KEY] = mode.name }
        }

        suspend fun setAdvisorMinSizeMb(mb: Int) {
            context.dataStore.edit { prefs -> prefs[ADVISOR_MIN_SIZE_MB_KEY] = mb }
        }

        suspend fun setAdvisorUnusedMonths(months: Int) {
            context.dataStore.edit { prefs -> prefs[ADVISOR_UNUSED_MONTHS_KEY] = months }
        }

        suspend fun setLastSeenWhatsNewVersion(versionCode: Int) {
            context.dataStore.edit { prefs -> prefs[LAST_SEEN_WHATS_NEW_VERSION_KEY] = versionCode }
        }

        suspend fun bumpChangeVersion() {
            context.dataStore.edit { prefs -> prefs[CHANGE_VERSION_KEY] = (prefs[CHANGE_VERSION_KEY] ?: 0L) + 1L }
        }

        private companion object {
            val APPEARANCE_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("appearance_mode")
            val DYNAMIC_COLOR_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("dynamic_color_enabled")
            val VIEW_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("view_mode")
            val ADVISOR_MIN_SIZE_MB_KEY: Preferences.Key<Int> = intPreferencesKey("advisor_min_size_mb")
            val ADVISOR_UNUSED_MONTHS_KEY: Preferences.Key<Int> = intPreferencesKey("advisor_unused_months")
            val LAST_SEEN_WHATS_NEW_VERSION_KEY: Preferences.Key<Int> = intPreferencesKey("last_seen_whats_new_version")
            val CHANGE_VERSION_KEY: Preferences.Key<Long> = longPreferencesKey("file_index_change_version")
        }
    }

internal fun appearanceModeFromStoredValue(value: String?): AppearanceMode =
    value?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM

internal fun viewModeFromStoredValue(value: String?): ViewMode =
    value?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.LIST
