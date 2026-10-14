package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Repo

/**
 * The chat's top bar: back, the chat's picture and name (tap for chat details, AII-80), what the team is on
 * (AII-112/113), find (AII-78) and the menu with Mute. [focusBubble] is the newest message of the card in focus, if any.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTopBar(
    ref: ChatRef, repo: Repo, focus: String?, focusBubble: Bubble?, working: Boolean, workingOn: String?, muted: Boolean,
    onBack: () -> Unit, onDetails: () -> Unit, onFind: () -> Unit, onMute: () -> Unit, onGoToDate: () -> Unit = {}, onMyLast: () -> Unit = {}, onOpenCard: (String) -> Unit = {}, silent: Boolean = false, onSilent: () -> Unit = {},
) {
    TopAppBar(
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onDetails() }.testTag("chat-title")) {
                Avatar(ref, repo, 36.dp)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(if (focus != null) "$focus ${Mapper.clean(focusBubble?.cardTitle ?: "")}" else ref.name,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val on = workingOn.takeIf { working && focus == null }
                    CompositionLocalProvider(LocalViewConfiguration provides NoTouchPadding(LocalViewConfiguration.current)) { Text(if (on != null) stringResource(R.string.working_on, on) else if (focus == null) (if (working) stringResource(R.string.working) else ref.subtitle) else ref.name + if (focusBubble?.waiting == true) " · ⏳ " + stringResource(R.string.waiting_on_you) else "", style = MaterialTheme.typography.labelMedium,
                        color = if (focus == null && working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        // AII-219: tap opens that card. No 48 dp touch padding (NoTouchPadding), so taps beside it still reach the name (AII-113).
                        modifier = if (on != null) Modifier.testTag("working-on").clickable(onClickLabel = stringResource(R.string.open_working_card, on)) { onOpenCard(on) } else Modifier,
                        maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        },
        actions = {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = onFind) { Icon(Icons.Default.Search, stringResource(R.string.find_in_chat)) }
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.go_to_date)) },
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    onClick = { menu = false; onGoToDate() },
                    modifier = Modifier.testTag("go-to-date"),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(if (muted) R.string.unmute else R.string.mute_alerts)) },
                    leadingIcon = { Icon(if (muted) Icons.Default.Notifications else Icons.Default.NotificationsOff, null) },
                    onClick = { onMute(); menu = false },
                    modifier = Modifier.testTag("mute"),
                )
                DropdownMenuItem(
                    text = { Text(stringResource(if (silent) R.string.silent_alerts_off else R.string.silent_alerts)) },
                    leadingIcon = { Icon(Icons.Default.NotificationsOff, null) },
                    onClick = { onSilent(); menu = false },
                    modifier = Modifier.testTag("silent"),
                )
            }
        },
    )
}

/** AII-219: Compose widens small tap targets to 48 dp; for the working-on line that would steal taps meant for the chat name (AII-113). */
private class NoTouchPadding(base: ViewConfiguration) : ViewConfiguration by base {
    override val minimumTouchTargetSize: DpSize get() = DpSize.Zero
}
