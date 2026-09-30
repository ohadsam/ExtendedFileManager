package com.efm.filemanager.ui.feature.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Whether this device has biometric hardware enrolled and ready -- the vault's biometric option only ever shows when this is true. */
internal fun biometricAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Shows the system biometric prompt. Requires a [FragmentActivity] host -- [MainActivity][com.efm.filemanager.MainActivity]
 * extends `AppCompatActivity`, itself a `FragmentActivity`, so every screen's `LocalContext.current` already qualifies.
 */
internal fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    negativeButtonText: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val callback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) = onError(errString.toString())
        }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
    val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setNegativeButtonText(negativeButtonText).build()
    prompt.authenticate(info)
}
