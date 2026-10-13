package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.data.CardDetail

/** The board's column names, as Rich knows them. */
fun statusLabel(status: String) = Words.status[status] ?: status.replaceFirstChar { it.uppercase() }

/** The top of a card's view (AII-45): where it is on the board, what it's about, and the steps to test it. */
@Composable
fun CardDetails(c: CardDetail) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainer, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("card-details")) {
        Column(Modifier.padding(14.dp)) {
            Text(statusLabel(c.status) + if (c.assignee.isNotBlank()) " · ${c.assignee}" else "",
                style = MaterialTheme.typography.labelMedium, color = cs.primary)
            if (c.description.isNotBlank()) Text(Markdown.render(c.description.trim(), cs.surfaceVariant, cs.primary),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            if (c.testSteps.isNotBlank()) {
                Text(stringResource(R.string.card_to_test), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                Text(Markdown.render(c.testSteps.trim(), cs.surfaceVariant, cs.primary), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
