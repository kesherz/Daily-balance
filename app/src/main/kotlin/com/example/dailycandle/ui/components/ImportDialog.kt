package com.example.dailycandle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.dailycandle.R
import com.example.dailycandle.domain.CsvFailure
import com.example.dailycandle.domain.CsvImport
import com.example.dailycandle.domain.CsvProblem
import com.example.dailycandle.domain.DailyEntry
import com.example.dailycandle.domain.DuplicatePolicy
import com.example.dailycandle.ui.screens.RadioSetting
import com.example.dailycandle.ui.theme.Space

@Composable
fun ImportDialog(preview: CsvImport, existing: List<DailyEntry>, busy: Boolean, onImport: (DuplicatePolicy) -> Unit, onDismiss: () -> Unit) {
    var replace by rememberSaveable(preview) { mutableStateOf(false) }
    val existingCount = remember(preview, existing) {
        val dates = existing.map { it.date }.toSet()
        preview.entries.count { it.date in dates }
    }
    val heightLimit = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * 0.5f }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            LazyColumn(Modifier.heightIn(max = heightLimit).testTag("import_preview"), verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                if (preview.failure != null) item {
                    Text(stringResource(if (preview.failure == CsvFailure.HEADER) R.string.import_header_error else R.string.import_size_error), color = MaterialTheme.colorScheme.error)
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.small)) {
                            Text(stringResource(R.string.import_counts, preview.entries.size, preview.invalidRows))
                            Text(stringResource(R.string.import_existing, existingCount))
                        }
                    }
                    if (existingCount > 0) item {
                        Column {
                            SectionTitle(stringResource(R.string.import_policy))
                            RadioSetting(stringResource(R.string.keep_existing), !replace, !busy, { replace = false })
                            RadioSetting(stringResource(R.string.replace_existing), replace, !busy, { replace = true })
                        }
                    }
                    item { Text(stringResource(R.string.import_duplicates), style = MaterialTheme.typography.bodySmall) }
                    if (preview.entries.isEmpty()) item { Text(stringResource(R.string.import_no_entries)) }
                    items(preview.issues, key = { it.line }) { issue ->
                        val reason = stringResource(when (issue.problem) {
                            CsvProblem.COLUMNS -> R.string.import_columns_error
                            CsvProblem.DATE -> R.string.import_date_error
                            CsvProblem.VALUE -> R.string.import_value_error
                            CsvProblem.DUPLICATE_DATE -> R.string.import_duplicate_error
                        })
                        Text(stringResource(R.string.import_line_error, issue.line, reason), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (preview.invalidRows > preview.issues.size) item { Text(stringResource(R.string.import_more_errors, preview.issues.size)) }
                }
            }
        },
        confirmButton = {
            if (preview.failure == null && preview.entries.isNotEmpty()) TextButton(
                onClick = { onImport(if (replace) DuplicatePolicy.REPLACE_EXISTING else DuplicatePolicy.KEEP_EXISTING) },
                enabled = !busy, modifier = Modifier.testTag("confirm_import"),
            ) { Text(pluralStringResource(R.plurals.import_apply, preview.entries.size, preview.entries.size)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
    )
}
