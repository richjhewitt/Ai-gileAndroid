package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.ColumnScope
import com.smizflark.aigile.messenger.R
import androidx.compose.ui.res.stringResource
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.smizflark.aigile.messenger.data.Store

/* ---------- Settings: Look rows (AII-388 split) ---------- */

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.SettingsLookRows(s: Store, rLook: List<String>, rTimes: List<String>, rGifs: List<String>, rPics: List<String>, rCompact: List<String>, rMuted: List<String>, rSort: List<String>, rSwipeR: List<String>, rSwipeL: List<String>) {
    SettingRow(rLook) { AppearanceSettings(s) }
    SettingRow(rTimes) { Toggle(stringResource(R.string.show_times), stringResource(R.string.show_times_sub), Appearance.showTimes, Modifier.testTag("showTimes")) {
        Appearance.showTimes = it; s.showTimes = it
    } }
    SettingRow(rGifs) { Toggle(stringResource(R.string.play_gifs), stringResource(R.string.play_gifs_sub), Appearance.gifs, Modifier.testTag("gifs")) {
        Appearance.gifs = it; s.gifs = it
    } }
    SettingRow(rPics) { LoadPicturesPick(Appearance.loadPictures) { Appearance.loadPictures = it; s.loadPictures = it } }
    SettingRow(rCompact) { Toggle(stringResource(R.string.compact_list), stringResource(R.string.compact_list_sub), Appearance.compact, Modifier.testTag("compact")) {
        Appearance.compact = it; s.compact = it
    } }
    SettingRow(rMuted) { Toggle(stringResource(R.string.muted_last), stringResource(R.string.muted_last_sub), Appearance.mutedLast, Modifier.testTag("mutedLast")) {
        Appearance.mutedLast = it; s.mutedLast = it
    } }
    SettingRow(rSort) { SortPick(Appearance.sort) { Appearance.sort = it; s.sortChats = it } }
    SettingRow(rSwipeR) { SwipePick(stringResource(R.string.swipe_right), Appearance.swipeRight, "swipeRight") { Appearance.swipeRight = it; s.swipeRight = it.id } }
    SettingRow(rSwipeL) { SwipePick(stringResource(R.string.swipe_left), Appearance.swipeLeft, "swipeLeft") { Appearance.swipeLeft = it; s.swipeLeft = it.id } }
}
