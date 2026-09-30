package com.efm.filemanager.ui.feature.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.efm.filemanager.R

/** The password/biometric gate shown in place of the vault's contents until [VaultViewModel.isUnlocked]. */
@Composable
internal fun VaultGateBody(
    isPasswordSet: Boolean,
    biometricEnabled: Boolean,
    onSetPassword: (CharArray, CharArray) -> Unit,
    onUnlock: (CharArray) -> Unit,
    onBiometricUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
        if (isPasswordSet) {
            VaultUnlockForm(onUnlock = onUnlock, biometricEnabled = biometricEnabled, onBiometricUnlock = onBiometricUnlock)
        } else {
            VaultSetupForm(onSetPassword)
        }
    }
}

@Composable
private fun VaultSetupForm(onSetPassword: (CharArray, CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    Text(stringResource(R.string.vault_setup_title), style = MaterialTheme.typography.titleMedium)
    Text(
        text = stringResource(R.string.vault_setup_body),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
    )
    PasswordField(value = password, onValueChange = { password = it }, label = stringResource(R.string.vault_password_label))
    PasswordField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = stringResource(R.string.vault_confirm_password_label),
        modifier = Modifier.padding(top = 8.dp),
    )
    Button(
        onClick = { onSetPassword(password.toCharArray(), confirmPassword.toCharArray()) },
        enabled = password.isNotEmpty() && confirmPassword.isNotEmpty(),
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.vault_setup_button))
    }
}

@Composable
private fun VaultUnlockForm(
    onUnlock: (CharArray) -> Unit,
    biometricEnabled: Boolean,
    onBiometricUnlock: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val showBiometricOption = biometricEnabled && remember { biometricAvailable(context) }

    Text(stringResource(R.string.vault_unlock_title), style = MaterialTheme.typography.titleMedium)
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.vault_password_label),
        modifier = Modifier.padding(top = 16.dp),
    )
    Button(
        onClick = { onUnlock(password.toCharArray()) },
        enabled = password.isNotEmpty(),
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.vault_unlock_button))
    }
    if (showBiometricOption) {
        BiometricUnlockButton(onUnlocked = onBiometricUnlock)
    }
}

@Composable
private fun BiometricUnlockButton(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val promptTitle = stringResource(R.string.vault_unlock_title)
    val cancelLabel = stringResource(R.string.close)
    TextButton(
        onClick = {
            val activity = context as? FragmentActivity ?: return@TextButton
            showBiometricPrompt(activity, promptTitle, cancelLabel, onSuccess = onUnlocked, onError = {})
        },
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(stringResource(R.string.vault_use_biometric))
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = modifier.fillMaxWidth(),
    )
}
