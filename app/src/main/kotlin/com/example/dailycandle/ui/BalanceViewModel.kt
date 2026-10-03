package com.example.dailycandle.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailycandle.R
import com.example.dailycandle.data.BalanceRepository
import com.example.dailycandle.data.DataTransfer
import com.example.dailycandle.data.EntryConflictException
import com.example.dailycandle.data.MigrationReceipt
import com.example.dailycandle.data.ThemePreference
import com.example.dailycandle.data.UserPreferences
import com.example.dailycandle.domain.AmountInput
import com.example.dailycandle.domain.Candle
import com.example.dailycandle.domain.CsvImport
import com.example.dailycandle.domain.DailyEntry
import com.example.dailycandle.domain.DuplicatePolicy
import com.example.dailycandle.domain.Summary
import com.example.dailycandle.domain.buildCandles
import com.example.dailycandle.domain.summarize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class Destination { HOME, HISTORY, SETTINGS }
data class EntryDraft(
    val date: LocalDate,
    val text: String = "",
    val expectedUpdate: Long? = null,
    val dateLocked: Boolean = false,
    val sheet: Boolean = false,
    val error: Int? = null,
)
data class BalanceState(
    val loading: Boolean = true,
    val failure: Int? = null,
    val busy: Boolean = false,
    val entries: List<DailyEntry> = emptyList(),
    val candles: List<Candle> = emptyList(),
    val summary: Summary? = null,
    val migration: MigrationReceipt? = null,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val destination: Destination = Destination.HOME,
    val selectedDate: LocalDate? = null,
    val draft: EntryDraft = EntryDraft(LocalDate.now()),
    val importPreview: CsvImport? = null,
    val pendingUndo: DailyEntry? = null,
)
sealed interface UiEvent {
    data class Message(val resource: Int, val arguments: List<Any> = emptyList()) : UiEvent
}

class BalanceViewModel(
    private val repository: BalanceRepository,
    private val preferences: UserPreferences,
    private val transfer: DataTransfer,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(BalanceState(
        destination = savedState.get<String>("destination")?.let {
            runCatching { Destination.valueOf(it) }.getOrNull()
        } ?: Destination.HOME,
        selectedDate = savedState.get<Long>("selection")?.let(LocalDate::ofEpochDay),
        draft = EntryDraft(
            date = savedState.get<Long>("draft_date")?.let(LocalDate::ofEpochDay) ?: LocalDate.now(),
            text = savedState["draft_text"] ?: "",
            expectedUpdate = savedState["draft_update"],
            dateLocked = savedState["draft_locked"] ?: false,
            sheet = savedState["draft_sheet"] ?: false,
        ),
        pendingUndo = savedState.get<Long>("undo_date")?.let { day ->
            savedState.get<String>("undo_value")?.let(AmountInput::parseCanonical)?.let { amount ->
                DailyEntry(LocalDate.ofEpochDay(day), amount, savedState["undo_created"] ?: 0, savedState["undo_updated"] ?: 0)
            }
        },
    ))
    val state = _state.asStateFlow()
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        retry()
        viewModelScope.launch { preferences.theme.collect { theme -> _state.update { it.copy(theme = theme) } } }
    }

    fun retry() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, failure = null) }
            try {
                val receipt = repository.initialize()
                _state.update { it.copy(migration = receipt) }
                repository.entries.collect { entries ->
                    val computed = withContext(Dispatchers.Default) { buildCandles(entries) to summarize(entries) }
                    _state.update { old ->
                        val selected = old.selectedDate?.takeIf { date -> entries.any { it.date == date } }
                            ?: entries.lastOrNull()?.date
                        val existing = entries.firstOrNull { it.date == old.draft.date }
                        old.copy(
                            entries = entries, candles = computed.first, summary = computed.second,
                            selectedDate = selected, loading = false,
                            draft = if (old.draft.text.isEmpty() && !old.draft.sheet)
                                old.draft.copy(expectedUpdate = existing?.updatedAt) else old.draft,
                        )
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update { it.copy(loading = false, failure = R.string.error_load) }
            }
        }
    }

    fun navigate(destination: Destination) {
        savedState["destination"] = destination.name
        _state.update { it.copy(destination = destination) }
    }

    fun select(date: LocalDate) {
        savedState["selection"] = date.toEpochDay()
        _state.update { it.copy(selectedDate = date) }
    }

    private fun draft(value: EntryDraft) {
        savedState["draft_date"] = value.date.toEpochDay()
        savedState["draft_text"] = value.text
        savedState["draft_update"] = value.expectedUpdate
        savedState["draft_locked"] = value.dateLocked
        savedState["draft_sheet"] = value.sheet
        _state.update { it.copy(draft = value) }
    }

    fun setValue(text: String) { draft(_state.value.draft.copy(text = text, error = null)) }
    fun setDate(date: LocalDate) {
        val existing = _state.value.entries.firstOrNull { it.date == date }
        draft(_state.value.draft.copy(
            date = date, expectedUpdate = existing?.updatedAt,
            text = existing?.value?.let(AmountInput::canonical) ?: _state.value.draft.text, error = null,
        ))
    }

    fun edit(entry: DailyEntry) {
        draft(EntryDraft(entry.date, AmountInput.canonical(entry.value), entry.updatedAt, dateLocked = true, sheet = true))
    }

    fun newEntry() {
        val existing = _state.value.entries.firstOrNull { it.date == LocalDate.now() }
        draft(EntryDraft(LocalDate.now(), expectedUpdate = existing?.updatedAt, sheet = true))
    }

    fun dismissEditor() { draft(EntryDraft(LocalDate.now(), expectedUpdate = todayEntry()?.updatedAt)) }
    private fun todayEntry() = _state.value.entries.firstOrNull { it.date == LocalDate.now() }

    private fun operation(onError: (Int) -> Unit = {}, action: suspend () -> Unit) {
        if (_state.value.busy || _state.value.loading || _state.value.failure != null) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                val message = if (error is EntryConflictException) R.string.error_conflict else R.string.error_operation
                onError(message)
                eventChannel.send(UiEvent.Message(message))
            } finally { _state.update { it.copy(busy = false) } }
        }
    }

    fun save() {
        val current = _state.value.draft
        val value = AmountInput.parse(current.text)
        if (value == null) { draft(current.copy(error = R.string.invalid_value)); return }
        operation(onError = { message -> draft(_state.value.draft.copy(error = message)) }) {
            repository.save(current.date, value, current.expectedUpdate)
            val observed = _state.first { state ->
                state.failure != null || state.entries.any { entry ->
                    entry.date == current.date && entry.value.compareTo(value) == 0 &&
                        (current.expectedUpdate == null || entry.updatedAt > current.expectedUpdate)
                }
            }
            check(observed.failure == null)
            select(current.date)
            dismissEditor()
            navigate(Destination.HOME)
            eventChannel.send(UiEvent.Message(if (current.expectedUpdate == null) R.string.entry_saved else R.string.entry_updated))
        }
    }

    fun delete(entry: DailyEntry) = operation {
        repository.delete(entry)
        savedState["undo_date"] = entry.date.toEpochDay()
        savedState["undo_value"] = AmountInput.canonical(entry.value)
        savedState["undo_created"] = entry.createdAt
        savedState["undo_updated"] = entry.updatedAt
        _state.update { it.copy(pendingUndo = entry) }
    }

    fun dismissUndo(entry: DailyEntry) {
        if (_state.value.pendingUndo != entry) return
        listOf("undo_date", "undo_value", "undo_created", "undo_updated").forEach { savedState.remove<Any>(it) }
        _state.update { it.copy(pendingUndo = null) }
    }

    fun undo(entry: DailyEntry) = operation {
        val restored = repository.restore(entry)
        dismissUndo(entry)
        eventChannel.send(UiEvent.Message(if (restored) R.string.entry_restored else R.string.error_restore_conflict))
    }

    fun theme(value: ThemePreference) = operation { preferences.setTheme(value) }

    fun preview(uri: Uri) = operation {
        val preview = transfer.preview(uri)
        _state.update { it.copy(importPreview = preview) }
    }
    fun dismissImport() { _state.update { it.copy(importPreview = null) } }

    fun import(policy: DuplicatePolicy) {
        val preview = _state.value.importPreview ?: return
        if (preview.failure != null || preview.entries.isEmpty()) return
        operation {
            val result = repository.import(preview, policy)
            dismissImport()
            eventChannel.send(UiEvent.Message(R.string.import_finished, listOf(result.written, result.skipped)))
        }
    }

    fun export(uri: Uri, legacy: Boolean) = operation {
        transfer.export(uri, legacy)
        eventChannel.send(UiEvent.Message(R.string.export_finished))
    }
}
