package com.example.dailycandle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.dailycandle.data.BalanceDatabase
import com.example.dailycandle.data.BalanceRepository
import com.example.dailycandle.data.DataTransfer
import com.example.dailycandle.data.LegacySource
import com.example.dailycandle.data.ThemePreference
import com.example.dailycandle.data.UserPreferences
import com.example.dailycandle.domain.LegacySnapshot
import com.example.dailycandle.ui.BalanceApp
import com.example.dailycandle.ui.BalanceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Exercises the real Compose -> ViewModel -> Room path in an isolated database. */
@RunWith(AndroidJUnit4::class)
class BalanceWorkflowTest {
    @Rule @JvmField val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val models = ViewModelStore()
    private lateinit var database: BalanceDatabase
    private lateinit var repository: BalanceRepository
    private lateinit var model: BalanceViewModel
    private lateinit var restoration: StateRestorationTester

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, BalanceDatabase::class.java).build()
        repository = BalanceRepository(database, LegacySource { LegacySnapshot("", "") })
    }

    @After fun close() {
        compose.runOnIdle { models.clear() }
        database.close()
    }

    private fun launch() {
        val preferences = UserPreferences(context)
        runBlocking { preferences.setTheme(ThemePreference.LIGHT) }
        val factory = viewModelFactory {
            initializer { BalanceViewModel(repository, preferences, DataTransfer(context.contentResolver, repository), SavedStateHandle()) }
        }
        compose.runOnIdle { model = ViewModelProvider(models, factory)[BalanceViewModel::class.java] }
        restoration = StateRestorationTester(compose)
        restoration.setContent { BalanceApp(model) }
        awaitReady()
    }

    private fun awaitReady() = compose.waitUntil(10_000) { !model.state.value.loading && !model.state.value.busy }

    private fun awaitEntries(count: Int) = compose.waitUntil(10_000) {
        model.state.value.entries.size == count && !model.state.value.busy
    }

    @Test fun quickEntryValidatesDecimalsAndUpdatesTheSameDay() {
        launch()
        compose.onNodeWithText(context.getString(R.string.empty_title)).assertIsDisplayed()
        compose.onNodeWithTag("entry_value").performTextReplacement("NaN")
        compose.onNodeWithTag("save_entry").performClick()
        compose.runOnIdle {
            assertEquals(R.string.invalid_value, model.state.value.draft.error)
            assertTrue(model.state.value.entries.isEmpty())
        }
        compose.onNodeWithTag("entry_value").performTextReplacement("-0,125")
        compose.onNodeWithTag("save_entry").performClick()
        awaitEntries(1)
        compose.onNodeWithText(context.getString(R.string.first_title)).assertIsDisplayed()
        compose.runOnIdle { assertEquals("-0.125", model.state.value.entries.single().value.toPlainString()) }

        compose.onNodeWithTag("save_entry").assertTextContains(context.getString(R.string.update_entry))
        compose.onNodeWithTag("entry_value").performTextReplacement("3.125000000001")
        compose.onNodeWithTag("save_entry").performClick()
        compose.waitUntil(10_000) { model.state.value.entries.singleOrNull()?.value?.toPlainString() == "3.125000000001" }
        compose.runOnIdle { assertEquals(1, model.state.value.entries.size) }

        compose.runOnIdle { model.setDate(LocalDate.now().minusDays(1)) }
        compose.onNodeWithTag("entry_value").performTextReplacement("0")
        compose.onNodeWithTag("save_entry").performClick()
        awaitEntries(2)
        compose.runOnIdle {
            assertEquals(LocalDate.now(), model.state.value.candles.single().date)
            assertEquals(null, model.state.value.candles.single().change.percent)
        }
        // Selection must wait for the newly saved record, rather than fall back to old chart data.
        val next = LocalDate.now().plusDays(1)
        compose.runOnIdle { model.setDate(next) }
        compose.onNodeWithTag("entry_value").performTextReplacement("5.25")
        compose.onNodeWithTag("save_entry").performClick()
        awaitEntries(3)
        compose.runOnIdle { assertEquals(next, model.state.value.selectedDate) }
    }

    @Test fun historyEditsDeletesAndUndoesWithoutResettingData() {
        val day = LocalDate.of(2026, 1, 1)
        runBlocking {
            repository.save(day, "10".toBigDecimal(), null)
            repository.save(day.plusDays(1), "12".toBigDecimal(), null)
        }
        launch()
        compose.onNodeWithTag("nav_history").performClick()
        compose.onNodeWithTag("edit_${day.toEpochDay()}").performClick()
        compose.onNodeWithTag("entry_value").performTextReplacement("-9.75")
        compose.onNodeWithTag("dialog_save").performClick()
        compose.waitUntil(10_000) { model.state.value.entries.firstOrNull()?.value?.toPlainString() == "-9.75" }
        compose.onNodeWithTag("latest_value").assertIsDisplayed()
        compose.onNodeWithTag("nav_history").performClick()
        compose.onNodeWithTag("delete_${day.toEpochDay()}").performClick()
        compose.onNodeWithTag("confirm_delete").performClick()
        awaitEntries(1)
        compose.waitUntil(10_000) { compose.onAllNodesWithText(context.getString(R.string.undo)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(context.getString(R.string.undo)).performClick()
        awaitEntries(2)
        compose.runOnIdle { assertEquals("-9.75", model.state.value.entries.first().value.toPlainString()) }
    }

    @Test fun chartSelectionPanPinchAndDoubleTapResetUseStoredDates() {
        val start = LocalDate.of(2025, 6, 1)
        runBlocking {
            repeat(60) { index -> repository.save(start.plusDays(index.toLong() * 2), (100 + index).toBigDecimal(), null) }
        }
        launch()
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("balance_chart"))
        compose.onNodeWithTag("balance_chart").performTouchInput {
            click(Offset(width * 0.1f, height * 0.5f))
        }
        compose.waitUntil(10_000) { model.state.value.selectedDate != start.plusDays(118) }
        val selected = model.state.value.selectedDate
        compose.runOnIdle { assertTrue(selected in model.state.value.candles.map { it.date }) }
        val initialWindow = chartWindow()
        compose.onNodeWithTag("balance_chart").performTouchInput {
            down(0, Offset(width * 0.3f, height * 0.5f))
            down(1, Offset(width * 0.6f, height * 0.5f))
            repeat(10) { step ->
                moveTo(0, Offset(width * (0.3f - step * 0.02f), height * 0.5f))
                moveTo(1, Offset(width * (0.6f + step * 0.02f), height * 0.5f))
            }
            up(0); up(1)
        }
        val zoomedWindow = chartWindow()
        assertNotEquals(initialWindow, zoomedWindow)
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(zoomedWindow, chartWindow())
        compose.onNodeWithTag("balance_chart").performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.5f), Offset(width * 0.75f, height * 0.5f), 500)
        }
        assertNotEquals(zoomedWindow, chartWindow())
        compose.onNodeWithTag("balance_chart").performTouchInput {
            doubleClick(center)
        }
        compose.waitUntil(10_000) { model.state.value.selectedDate == start.plusDays(118) }
        assertEquals(initialWindow, chartWindow())
        compose.onNodeWithTag("balance_chart").assertIsDisplayed()
    }

    private fun chartWindow() = compose.onNodeWithTag("balance_chart").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
}
