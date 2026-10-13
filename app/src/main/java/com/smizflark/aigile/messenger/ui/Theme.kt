package com.smizflark.aigile.messenger.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Brand = Color(0xFF16A34A)

private val Light = lightColorScheme(
    primary = Brand, onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5E1), onPrimaryContainer = Color(0xFF0B3B1C),
    secondaryContainer = Color(0xFFEDEFF3), surface = Color(0xFFF7F7F9), background = Color(0xFFF7F7F9),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF4ADE80), onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF14532D), onPrimaryContainer = Color(0xFFD7F5E1),
    surface = Color(0xFF121314), background = Color(0xFF121314),
)

/** Follows the phone: light or dark, and the phone's own colours on Android 12+. */
@Composable
fun MessengerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
