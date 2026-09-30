package com.efm.filemanager.data.vault

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the vault's password/biometric gate has been passed this app process -- a plain
 * in-memory flag, never persisted, so it resets to locked on process death same as any other
 * "unlocked for this session" app pattern. Leaving the Vault screen and coming back within the
 * same session doesn't re-prompt; a fresh app launch always does.
 */
@Singleton
class VaultUnlockState
    @Inject
    constructor() {
        private val _isUnlocked = MutableStateFlow(false)
        val isUnlocked: StateFlow<Boolean> = _isUnlocked

        fun markUnlocked() {
            _isUnlocked.value = true
        }

        fun lock() {
            _isUnlocked.value = false
        }
    }
