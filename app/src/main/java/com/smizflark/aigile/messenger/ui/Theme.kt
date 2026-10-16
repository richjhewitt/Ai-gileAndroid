package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

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
enum class Look(val id: String, val label: String, @androidx.annotation.StringRes val res: Int) {
    System("system", "Follow the phone", R.string.look_system),
    Light("light", "Light", R.string.look_light),
    Dark("dark", "Dark", R.string.look_dark),
    Black("black", "Black (OLED)", R.string.look_black);

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: System }
}

/** The chosen look, live: Settings changes it and every screen follows at once. */
object Appearance {
    var look by mutableStateOf<Look?>(null)
    var phoneColours by mutableStateOf(true)
    var textSize by mutableStateOf(TextSize.Normal)
    /** Enter sends (AII-126): read by the message box. */
    var enterSends by mutableStateOf(false)
    var gifs by mutableStateOf(true)
    /** Load pictures (AII-391): "always" or "wifi". */
    var loadPictures by mutableStateOf("always")
    /** Link previews under bubbles (AII-341), off until switched on. */
    var linkPreviews by mutableStateOf(false)
    /** Compact chats list (AII-212). */
    var compact by mutableStateOf(false)
    /** Muted chats sink to the bottom of the chats list (AII-221). */
    var mutedLast by mutableStateOf(false)
    var sortAz by mutableStateOf(false)   // AII-241
    /** Undo send (AII-202): Send waits 5 seconds with an Undo bar. */
    var undoSend by mutableStateOf(false)
    var undoSeconds by mutableStateOf(5)
    var showTimes by mutableStateOf(false)
    /** What swiping a chat does (AII-216). */
    var swipeRight by mutableStateOf(com.smizflark.aigile.messenger.data.SWIPE_RIGHT_DEFAULT)
    var swipeLeft by mutableStateOf(com.smizflark.aigile.messenger.data.SWIPE_LEFT_DEFAULT)
    /** The Next unread button in a chat (AII-254). */
    var nextUnread by mutableStateOf(true)
    /** Buzz on send and on a new reply (AII-205). */
    var buzz by mutableStateOf(true)
    /** The hour Send in the morning uses (AII-208). */
    var morning by mutableStateOf(8)
    /** Quick replies above an empty message box (AII-179); off until loaded, so screenshots stay as they were. */
    var quickReplies by mutableStateOf(false)
    /** Your own quick replies (AII-180); empty means the starting ones. */
    var replies by mutableStateOf(emptyList<String>())

    fun load(store: Store) { if (look == null) reload(store) }

    /** Reads everything again, so the screen follows a reset at once (AII-217). */
    fun reload(store: Store) {
        run { look = Look.of(store.look); phoneColours = store.phoneColours; textSize = TextSize.of(store.textSize); enterSends = store.enterSends; gifs = store.gifs; loadPictures = store.loadPictures; linkPreviews = store.linkPreviews; compact = store.compact; mutedLast = store.mutedLast; sortAz = store.sortChats == "az"; morning = store.morning; buzz = store.buzz; undoSend = store.undoSend; undoSeconds = store.undoSeconds; quickReplies = store.quickReplies; showTimes = store.showTimes; swipeRight = com.smizflark.aigile.messenger.data.SwipeChoice.of(store.swipeRight, com.smizflark.aigile.messenger.data.SWIPE_RIGHT_DEFAULT); swipeLeft = com.smizflark.aigile.messenger.data.SwipeChoice.of(store.swipeLeft, com.smizflark.aigile.messenger.data.SWIPE_LEFT_DEFAULT); nextUnread = store.nextUnread; replies = com.smizflark.aigile.messenger.data.Replies.parse(store.quickReplyText) }
    }
}

/* ---------- text size ---------- */

/** The text sizes in Settings (AII-66); [id] is what the Store keeps. */
enum class TextSize(val id: String, val label: String, val scale: Float, @androidx.annotation.StringRes val res: Int) {
    Small("small", "Small", 0.85f, R.string.size_small),
    Normal("normal", "Normal", 1f, R.string.size_normal),
    Large("large", "Large", 1.15f, R.string.size_large),
    Huge("huge", "Huge", 1.3f, R.string.size_huge);

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: Normal }
}

/** Pinch to zoom (AII-259): the size after a pinch of [zoom] (>1 = fingers apart). At most one step; small pinches change nothing. */
fun nextTextSize(current: TextSize, zoom: Float): TextSize {
    val all = TextSize.entries
    val i = all.indexOf(current)
    return when {
        zoom >= 1.15f -> all[minOf(i + 1, all.lastIndex)]
        zoom <= 0.87f -> all[maxOf(i - 1, 0)]
        else -> current
    }
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
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    Column(Modifier.selectableGroup()) {
        Look.entries.forEach { l ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .selectable(selected = Appearance.look == l, role = Role.RadioButton) {
                        Appearance.look = l; store.look = l.id; com.smizflark.aigile.messenger.widget.LatestWidget.refresh(ctx)
                    }
                    .testTag("look:${l.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = Appearance.look == l, onClick = null)
                Text(stringResource(l.res), Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (Build.VERSION.SDK_INT >= 31) Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.phone_colours), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.phone_colours_sub), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = Appearance.phoneColours, onCheckedChange = { Appearance.phoneColours = it; store.phoneColours = it },
            modifier = Modifier.testTag("phoneColours"))
    }
    Text(stringResource(R.string.text_size), style = MaterialTheme.typography.titleMedium)
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
                Text(stringResource(t.res), Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
