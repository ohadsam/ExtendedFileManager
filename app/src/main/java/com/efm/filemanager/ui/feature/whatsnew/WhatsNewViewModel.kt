package com.efm.filemanager.ui.feature.whatsnew

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efm.filemanager.data.prefs.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsNewViewModel
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
        @ApplicationContext context: Context,
    ) : ViewModel() {
        private val currentVersionCode = currentAppVersionCode(context)
        private val _entriesToShow = MutableStateFlow<List<WhatsNewEntry>>(emptyList())
        val entriesToShow: StateFlow<List<WhatsNewEntry>> = _entriesToShow.asStateFlow()

        init {
            viewModelScope.launch {
                val lastSeen = preferencesRepository.lastSeenWhatsNewVersion.first()
                if (lastSeen == null) {
                    // Fresh install -- record the baseline now so a later real upgrade has something to compare against.
                    preferencesRepository.setLastSeenWhatsNewVersion(currentVersionCode)
                } else {
                    _entriesToShow.value = entriesToShowFor(lastSeen, currentVersionCode)
                }
            }
        }

        /** Marks [currentVersionCode] as seen so the same entries don't show again on the next launch. */
        fun dismiss() {
            viewModelScope.launch {
                preferencesRepository.setLastSeenWhatsNewVersion(currentVersionCode)
                _entriesToShow.value = emptyList()
            }
        }
    }

private fun currentAppVersionCode(context: Context): Int {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
}
