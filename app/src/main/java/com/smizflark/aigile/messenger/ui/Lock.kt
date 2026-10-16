package com.smizflark.aigile.messenger.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Lock the app with fingerprint, face or phone PIN (AII-63). */
object Lock {
    /** How long the app can be in the background before it asks again. */
    const val GRACE_MS = 60_000L

    /**
     * Should the locked screen show now? [unlocked] is whether this run has been unlocked,
     * [leftAt] is when the app last went to the background (null if never), on the same clock as [now].
     */
    fun shouldLock(enabled: Boolean, unlocked: Boolean, leftAt: Long?, now: Long): Boolean =
        enabled && (!unlocked || (leftAt != null && now - leftAt > GRACE_MS))

    private const val ALLOWED = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** Can this phone check a fingerprint, face or PIN? */
    fun available(context: Context) =
        BiometricManager.from(context).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS

    /** Asks for fingerprint, face or PIN; [onDone] runs only on success, a cancel just leaves it locked. */
    fun ask(activity: FragmentActivity, onDone: () -> Unit) {
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onDone()
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Messenger")
            .setAllowedAuthenticators(ALLOWED)
            .build())
    }
}

/** Covers the whole app until unlocked. */
@Composable
fun LockedScreen(onUnlock: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Messenger is locked", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onUnlock, modifier = Modifier.padding(top = 16.dp)) { Text("Unlock") }
    }
}
