package com.smizflark.aigile.messenger.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.GoToDate
import com.smizflark.aigile.messenger.data.Now
import java.time.LocalDate

/** AII-193: pick a day between the chat's first and last message. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoToDateDialog(days: Collection<LocalDate>, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val range = GoToDate.range(days, Now.today())
    if (range == null) { onDismiss(); return }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = GoToDate.utcMillis(range.endInclusive),
        yearRange = range.start.year..range.endInclusive.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = GoToDate.pickedDay(utcTimeMillis) in range
            override fun isSelectableYear(year: Int) = year in range.start.year..range.endInclusive.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(GoToDate.pickedDay(it)) } ?: onDismiss() }) { Text(stringResource(R.string.done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state) }
}
