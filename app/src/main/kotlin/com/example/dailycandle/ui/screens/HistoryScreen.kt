package com.example.dailycandle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dailycandle.R
import com.example.dailycandle.domain.DailyEntry
import com.example.dailycandle.ui.components.AmountText
import com.example.dailycandle.ui.components.AppIcon
import com.example.dailycandle.ui.components.displayLocale
import com.example.dailycandle.ui.format.DisplayFormat
import com.example.dailycandle.ui.theme.Space

@Composable
fun HistoryScreen(entries: List<DailyEntry>, busy: Boolean, onEdit: (DailyEntry) -> Unit, onDelete: (DailyEntry) -> Unit, modifier: Modifier = Modifier) {
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var deletingDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val deleting = entries.firstOrNull { it.date.toEpochDay() == deletingDay }
    val locale = displayLocale()
    LazyColumn(modifier.testTag("history_list"), contentPadding = PaddingValues(start = Space.large, end = Space.large, top = Space.small, bottom = 96.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.small)) {
                Text(pluralStringResource(R.plurals.entry_count, entries.size, entries.size), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { newestFirst = !newestFirst }, modifier = Modifier.testTag("sort_history")) {
                    Text(stringResource(if (newestFirst) R.string.newest_first else R.string.oldest_first))
                }
            }
        }
        if (entries.isEmpty()) item { Text(stringResource(R.string.history_empty), Modifier.padding(vertical = Space.section)) }
        items(if (newestFirst) entries.asReversed() else entries, key = { it.date.toEpochDay() }) { entry ->
            Column(Modifier.animateItem()) {
                Row(Modifier.fillMaxWidth().padding(vertical = Space.medium), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable(enabled = !busy, onClickLabel = stringResource(R.string.edit_entry)) { onEdit(entry) }.padding(vertical = Space.small), verticalArrangement = Arrangement.spacedBy(Space.tiny)) {
                        Text(DisplayFormat.date(entry.date, locale), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AmountText(entry.value, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { onEdit(entry) }, enabled = !busy, modifier = Modifier.testTag("edit_${entry.date.toEpochDay()}")) {
                        AppIcon(R.drawable.ic_edit, description = stringResource(R.string.edit_entry))
                    }
                    IconButton(onClick = { deletingDay = entry.date.toEpochDay() }, enabled = !busy, modifier = Modifier.testTag("delete_${entry.date.toEpochDay()}")) {
                        AppIcon(R.drawable.ic_delete, description = stringResource(R.string.delete_entry))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    if (deleting != null) AlertDialog(
        onDismissRequest = { deletingDay = null },
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_body, DisplayFormat.value(deleting.value, locale), DisplayFormat.date(deleting.date, locale))) },
        confirmButton = { TextButton(onClick = { deletingDay = null; onDelete(deleting) }, modifier = Modifier.testTag("confirm_delete")) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deletingDay = null }) { Text(stringResource(R.string.cancel)) } },
    )
}
