package com.example.dailycandle.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailycandle.R
import com.example.dailycandle.ui.components.AppIcon
import com.example.dailycandle.ui.components.EntryDialog
import com.example.dailycandle.ui.components.EntryForm
import com.example.dailycandle.ui.components.ImportDialog
import com.example.dailycandle.ui.screens.HistoryScreen
import com.example.dailycandle.ui.screens.HomeScreen
import com.example.dailycandle.ui.screens.SettingsScreen
import com.example.dailycandle.ui.theme.BalanceTheme
import com.example.dailycandle.ui.theme.Space
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceApp(viewModel: BalanceViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbars = remember { SnackbarHostState() }
    val pages = rememberSaveableStateHolder()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.export(uri, legacy = false)
    }
    val legacyExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.export(uri, legacy = true)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.preview(uri)
    }
    LaunchedEffect(viewModel, resources) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbars.showSnackbar(resources.getString(event.resource, *event.arguments.toTypedArray()))
            }
        }
    }
    val undoLabel = stringResource(R.string.undo)
    val deletedLabel = stringResource(R.string.entry_deleted)
    LaunchedEffect(state.pendingUndo, undoLabel) {
        state.pendingUndo?.let { deleted ->
            snackbars.currentSnackbarData?.dismiss()
            val result = snackbars.showSnackbar(deletedLabel, undoLabel, withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(deleted)
            else viewModel.dismissUndo(deleted)
        }
    }
    BackHandler(state.destination != Destination.HOME && !state.draft.sheet && state.importPreview == null) {
        viewModel.navigate(Destination.HOME)
    }
    BalanceTheme(state.theme) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().semantics { testTagsAsResourceId = true }) {
            val wide = maxWidth >= 600.dp
            val captureLimit = maxHeight * 0.5f
            Row(Modifier.fillMaxSize()) {
                if (wide) NavigationRail(Modifier.safeDrawingPadding()) {
                    Destination.entries.forEach { destination ->
                        NavigationRailItem(
                            state.destination == destination, { viewModel.navigate(destination) },
                            icon = { AppIcon(destination.icon()) }, label = { Text(destination.label()) },
                            modifier = Modifier.testTag(destination.tag()),
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f), contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        Column {
                            TopAppBar(title = { Text(stringResource(if (state.destination == Destination.HOME) R.string.app_name else state.destination.labelResource())) })
                            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    },
                    bottomBar = {
                        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(
                            if (wide) WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            else WindowInsetsSides.Horizontal,
                        ))) {
                            if (state.destination == Destination.HOME && !state.draft.sheet) Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column {
                                    HorizontalDivider()
                                    EntryForm(
                                        state.draft, state.busy || state.loading || state.failure != null,
                                        viewModel::setValue, viewModel::setDate, viewModel::save,
                                        Modifier.fillMaxWidth().heightIn(max = captureLimit).verticalScroll(rememberScrollState()).padding(horizontal = Space.large, vertical = Space.small),
                                        compact = true,
                                    )
                                }
                            }
                            if (!wide) NavigationBar {
                                Destination.entries.forEach { destination ->
                                    NavigationBarItem(
                                        state.destination == destination, { viewModel.navigate(destination) },
                                        icon = { AppIcon(destination.icon()) }, label = { Text(destination.label()) },
                                        modifier = Modifier.testTag(destination.tag()),
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (state.destination == Destination.HISTORY && !state.loading && state.failure == null) {
                            FloatingActionButton(onClick = { if (!state.busy) viewModel.newEntry() }, modifier = Modifier.testTag("add_entry")) {
                                AppIcon(R.drawable.ic_add, description = stringResource(R.string.add_entry))
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbars) },
                ) { padding ->
                    val content = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                    when {
                        state.loading -> Column(content.padding(Space.section), verticalArrangement = Arrangement.spacedBy(Space.large)) {
                            Text(stringResource(R.string.loading))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        state.failure != null -> Column(content.padding(Space.section), verticalArrangement = Arrangement.spacedBy(Space.large)) {
                            Text(stringResource(state.failure!!), color = MaterialTheme.colorScheme.error)
                            Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                        }
                        else -> Crossfade(state.destination, modifier = content, animationSpec = tween(180), label = "navigation") { destination ->
                            pages.SaveableStateProvider(destination.name) {
                                when (destination) {
                                    Destination.HOME -> HomeScreen(state, viewModel::select, Modifier.fillMaxSize())
                                    Destination.HISTORY -> HistoryScreen(state.entries, state.busy, viewModel::edit, viewModel::delete, Modifier.fillMaxSize())
                                    Destination.SETTINGS -> SettingsScreen(
                                        state, viewModel::theme,
                                        onLanguage = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) },
                                        onExport = { export.launch("daily-balance-${LocalDate.now()}.csv") },
                                        onImport = { import.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
                                        onLegacyExport = { legacyExport.launch("daily-balance-original-records.csv") },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.draft.sheet) EntryDialog(state.draft, state.busy, viewModel::setValue, viewModel::setDate, viewModel::save, viewModel::dismissEditor)
        state.importPreview?.let { ImportDialog(it, state.entries, state.busy, viewModel::import, viewModel::dismissImport) }
    }
}

private fun Destination.labelResource() = when (this) {
    Destination.HOME -> R.string.nav_home
    Destination.HISTORY -> R.string.nav_history
    Destination.SETTINGS -> R.string.nav_settings
}
@Composable private fun Destination.label() = stringResource(labelResource())
private fun Destination.icon() = when (this) {
    Destination.HOME -> R.drawable.ic_chart
    Destination.HISTORY -> R.drawable.ic_history
    Destination.SETTINGS -> R.drawable.ic_settings
}
private fun Destination.tag() = "nav_${name.lowercase(Locale.ROOT)}"
