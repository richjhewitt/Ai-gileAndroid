package com.smizflark.aigile.messenger.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.smizflark.aigile.messenger.data.Store

val Brand = Color(0xFF16A34A)

/** Older phones (no Material You). Light primary is a shade darker than Brand so white text on it passes WCAG AA. */
internal val Light = lightColorScheme(
    primary = Color(0xFF15803D), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5E1), onPrimaryContainer = Color(0xFF0B3B1C),
    secondaryContainer = Color(0xFFEDEFF3), surface = Color(0xFFF7F7F9), background = Color(0xFFF7F7F9),
)
internal val Dark = darkColorScheme(
    primary = Color(0xFF4ADE80), onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF14532D), onPrimaryContainer = Color(0xFFD7F5E1),
    surface = Color(0xFF121314), background = Color(0xFF121314),
)

/* ---------- looks ---------- */

/** The looks in Settings; [id] is what the Store keeps. */
enum class Look(val id: String, val label: String) {
    System("system", "Follow the phone"),
    Light("light", "Light"),
    Dark("dark", "Dark"),
    Black("black", "Black (OLED)");

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: System }
}

/** The chosen look, live: Settings changes it and every screen follows at once. */
object Appearance {
    var look by mutableStateOf<Look?>(null)
    var phoneColours by mutableStateOf(true)
    var textSize by mutableStateOf(TextSize.Normal)

    fun load(store: Store) {
        if (look == null) { look = Look.of(store.look); phoneColours = store.phoneColours; textSize = TextSize.of(store.textSize) }
    }
}

/* ---------- text size ---------- */

/** The text sizes in Settings (AII-66); [id] is what the Store keeps. */
enum class TextSize(val id: String, val label: String, val scale: Float) {
    Small("small", "Small", 0.85f),
    Normal("normal", "Normal", 1f),
    Large("large", "Large", 1.15f),
    Huge("huge", "Huge", 1.3f);

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: Normal }
}

private fun TextStyle.times(k: Float) = copy(
    fontSize = if (fontSize.isSpecified) fontSize * k else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * k else lineHeight,
)

/** Every text style grown or shrunk by [k], so chats, messages and the rest of the app follow the setting. */
fun scaled(t: Typography, k: Float): Typography = if (k == 1f) t else t.copy(
    displayLarge = t.displayLarge.times(k), displayMedium = t.displayMedium.times(k), displaySmall = t.displaySmall.times(k),
    headlineLarge = t.headlineLarge.times(k), headlineMedium = t.headlineMedium.times(k), headlineSmall = t.headlineSmall.times(k),
    titleLarge = t.titleLarge.times(k), titleMedium = t.titleMedium.times(k), titleSmall = t.titleSmall.times(k),
    bodyLarge = t.bodyLarge.times(k), bodyMedium = t.bodyMedium.times(k), bodySmall = t.bodySmall.times(k),
    labelLarge = t.labelLarge.times(k), labelMedium = t.labelMedium.times(k), labelSmall = t.labelSmall.times(k),
)

fun isDark(look: Look, systemDark: Boolean) = when (look) {
    Look.System -> systemDark
    Look.Light -> false
    Look.Dark, Look.Black -> true
}

/** Pure black behind everything, for OLED screens; bars and bubbles stay just off black so they still stand out. */
fun blackOut(s: ColorScheme) = s.copy(
    background = Color.Black, surface = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0E0E0E), surfaceContainerHigh = Color(0xFF151515),
    surfaceContainerHighest = Color(0xFF1C1C1C), surfaceVariant = Color(0xFF1C1C1C),
    surfaceDim = Color.Black, surfaceBright = Color(0xFF1C1C1C),
)

/** The colours for a look; [phone] is the phone's own scheme (Android 12+, already light or dark) or null. */
fun schemeFor(look: Look, systemDark: Boolean, phone: ColorScheme?): ColorScheme {
    val base = phone ?: if (isDark(look, systemDark)) Dark else Light
    return if (look == Look.Black) blackOut(base) else base
}

/** Follows the chosen look (the phone's light or dark by default), with the phone's own colours on Android 12+. */
@Composable
fun MessengerTheme(store: Store? = null, content: @Composable () -> Unit) {
    store?.let { Appearance.load(it) }
    val look = Appearance.look ?: Look.System
    val systemDark = isSystemInDarkTheme()
    val dark = isDark(look, systemDark)
    val ctx = LocalContext.current
    val phone = if (Appearance.phoneColours && Build.VERSION.SDK_INT >= 31)
        (if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)) else null
    val scheme = schemeFor(look, systemDark, phone)

    // Status bar icons and the window behind the app follow the look, not just the phone's setting.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        window.decorView.setBackgroundColor(scheme.background.toArgb())
    }
    MaterialTheme(colorScheme = scheme, typography = scaled(Typography(), Appearance.textSize.scale), content = content)
}

/* ---------- Settings: Look ---------- */

@Composable
fun AppearanceSettings(store: Store) {
    Appearance.load(store)
    Text("Look", style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        Look.entries.forEach { l ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .selectable(selected = Appearance.look == l, role = Role.RadioButton) {
                        Appearance.look = l; store.look = l.id
                    }
                    .testTag("look:${l.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = Appearance.look == l, onClick = null)
                Text(l.label, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (Build.VERSION.SDK_INT >= 31) Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Phone's colours", style = MaterialTheme.typography.bodyLarge)
            Text("Match your wallpaper; off for AI-gile green", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = Appearance.phoneColours, onCheckedChange = { Appearance.phoneColours = it; store.phoneColours = it },
            modifier = Modifier.testTag("phoneColours"))
    }
    Text("Text size", style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        TextSize.entries.forEach { t ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .selectable(selected = Appearance.textSize == t, role = Role.RadioButton) {
                        Appearance.textSize = t; store.textSize = t.id
                    }
                    .testTag("textSize:${t.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = Appearance.textSize == t, onClick = null)
                Text(t.label, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
