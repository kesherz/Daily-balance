package com.example.dailycandle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.dailycandle.R
import com.example.dailycandle.domain.AmountInput
import com.example.dailycandle.ui.EntryDraft
import com.example.dailycandle.ui.format.DisplayFormat
import com.example.dailycandle.ui.theme.Space
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryForm(
    draft: EntryDraft,
    busy: Boolean,
    onValue: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    showSave: Boolean = true,
    compact: Boolean = false,
) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    val locale = displayLocale()
    val focus = LocalFocusManager.current
    val save = {
        onSave()
        if (AmountInput.parse(draft.text) != null) focus.clearFocus()
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.medium)) {
        OutlinedButton(
            onClick = { datePicker = true }, enabled = !busy && !draft.dateLocked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("entry_date"),
        ) {
            AppIcon(R.drawable.ic_calendar)
            Text(stringResource(R.string.recorded_on, DisplayFormat.date(draft.date, locale)), Modifier.padding(start = Space.small))
        }
        if (draft.expectedUpdate != null) {
            Text(stringResource(R.string.existing_entry, DisplayFormat.date(draft.date, locale)), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        val valueField: @Composable (Modifier) -> Unit = { fieldModifier -> OutlinedTextField(
            value = draft.text,
            onValueChange = { if (it.length <= AmountInput.MAX_TEXT_LENGTH) onValue(it) },
            label = { Text(stringResource(R.string.value_label)) },
            modifier = fieldModifier.testTag("entry_value"),
            enabled = !busy, singleLine = true,
            textStyle = TextStyle(textDirection = TextDirection.Ltr),
            isError = draft.error != null,
            supportingText = if (!compact) ({ Text(stringResource(draft.error ?: R.string.value_hint)) }) else null,
            leadingIcon = {
                IconButton(
                    onClick = {
                        onValue(if (draft.text.startsWith('-') || draft.text.startsWith('\u2212')) draft.text.drop(1)
                            else "-${draft.text.removePrefix("+")}")
                    },
                    enabled = !busy, modifier = Modifier.testTag("toggle_sign"),
                ) { AppIcon(R.drawable.ic_sign, description = stringResource(R.string.toggle_sign)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!busy) save() }),
        ) }
        val saveButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            Button(onClick = save, enabled = !busy, modifier = buttonModifier.heightIn(min = 48.dp).testTag("save_entry")) {
                Text(stringResource(if (draft.expectedUpdate == null) R.string.save_entry else R.string.update_entry))
            }
        }
        BoxWithConstraints {
            if (compact && showSave && maxWidth >= 340.dp && LocalDensity.current.fontScale <= 1.3f) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.small), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    valueField(Modifier.weight(1f))
                    saveButton(Modifier.heightIn(min = 56.dp))
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                valueField(Modifier.fillMaxWidth())
                if (showSave) saveButton(Modifier.fillMaxWidth())
            }
        }
        if (compact && draft.error != null) Text(stringResource(draft.error), color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
    if (datePicker) {
        // Material DatePicker uses UTC midnight, not the device timezone's midnight.
        val state = rememberDatePickerState(draft.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), yearRange = 1900..2100)
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    datePicker = false
                }, enabled = state.selectedDateMillis != null) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { datePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
}

@Composable
fun EntryDialog(draft: EntryDraft, busy: Boolean, onValue: (String) -> Unit, onDate: (LocalDate) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (draft.dateLocked) R.string.edit_entry else R.string.add_entry)) },
        text = {
            EntryForm(draft, busy, onValue, onDate, onSave, Modifier.verticalScroll(rememberScrollState()), showSave = false)
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !busy, modifier = Modifier.testTag("dialog_save")) {
                Text(stringResource(if (draft.expectedUpdate == null) R.string.save_entry else R.string.update_entry))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
    )
}
