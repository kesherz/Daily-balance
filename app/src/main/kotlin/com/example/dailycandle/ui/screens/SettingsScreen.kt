package com.example.dailycandle.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.dailycandle.R
import com.example.dailycandle.data.ThemePreference
import com.example.dailycandle.ui.BalanceState
import com.example.dailycandle.ui.components.AppIcon
import com.example.dailycandle.ui.components.SectionTitle
import com.example.dailycandle.ui.theme.Space

@Composable
fun SettingsScreen(
    state: BalanceState,
    onTheme: (ThemePreference) -> Unit,
    onLanguage: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLegacyExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    LazyColumn(modifier.testTag("settings_list"), contentPadding = PaddingValues(Space.large), verticalArrangement = Arrangement.spacedBy(Space.section)) {
        item {
            Column {
                SectionTitle(stringResource(R.string.appearance))
                ThemePreference.entries.forEach { theme ->
                    RadioSetting(stringResource(when (theme) {
                        ThemePreference.SYSTEM -> R.string.theme_system
                        ThemePreference.LIGHT -> R.string.theme_light
                        ThemePreference.DARK -> R.string.theme_dark
                    }), state.theme == theme, !state.busy, { onTheme(theme) }, Modifier.testTag("theme_${theme.name}"))
                }
            }
        }
        item {
            Column {
                SectionTitle(stringResource(R.string.language))
                RadioSetting(stringResource(R.string.language_system), language.isEmpty(), !state.busy, { onLanguage("") }, Modifier.testTag("language_system"))
                RadioSetting(stringResource(R.string.language_english), language.startsWith("en"), !state.busy, { onLanguage("en") }, Modifier.testTag("language_en"))
                RadioSetting(stringResource(R.string.language_persian), language.startsWith("fa"), !state.busy, { onLanguage("fa") }, Modifier.testTag("language_fa"))
            }
        }
        item { HorizontalDivider() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                SectionTitle(stringResource(R.string.data_tools))
                FilledTonalButton(onClick = onExport, enabled = !state.busy && state.entries.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export_csv")) {
                    AppIcon(R.drawable.ic_export)
                    Text(stringResource(R.string.export_csv), Modifier.padding(start = Space.small))
                }
                OutlinedButton(onClick = onImport, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("import_csv")) {
                    AppIcon(R.drawable.ic_import)
                    Text(stringResource(R.string.import_csv), Modifier.padding(start = Space.small))
                }
                Text(stringResource(R.string.csv_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val receipt = state.migration
        if (receipt != null && receipt.imported + receipt.rejected + receipt.duplicateDates + receipt.existingDates > 0) item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                SectionTitle(stringResource(R.string.legacy_title))
                Text(stringResource(R.string.legacy_report, receipt.imported, receipt.rejected, receipt.duplicateDates, receipt.existingDates), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.legacy_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onLegacyExport, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.export_legacy)) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                SectionTitle(stringResource(R.string.privacy_title))
                Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.percentage_basis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.calendar_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RadioSetting(label: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect).padding(vertical = Space.small),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.medium),
    ) {
        RadioButton(selected, onClick = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
