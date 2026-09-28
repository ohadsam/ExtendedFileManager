package com.efm.filemanager.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.efm.filemanager.domain.model.AppearanceMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "efm_preferences")

/**
 * Shared DataStore-backed preferences store -- later phases (view-mode defaults,
 * per-folder settings, ...) extend this instead of each adding their own DataStore.
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

        suspend fun setAppearanceMode(mode: AppearanceMode) {
            context.dataStore.edit { prefs -> prefs[APPEARANCE_MODE_KEY] = mode.name }
        }

        suspend fun setDynamicColorEnabled(enabled: Boolean) {
            context.dataStore.edit { prefs -> prefs[DYNAMIC_COLOR_KEY] = enabled }
        }

        private companion object {
            val APPEARANCE_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("appearance_mode")
            val DYNAMIC_COLOR_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("dynamic_color_enabled")
        }
    }

internal fun appearanceModeFromStoredValue(value: String?): AppearanceMode =
    value?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM
