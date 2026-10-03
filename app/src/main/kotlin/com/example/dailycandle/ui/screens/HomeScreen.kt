package com.example.dailycandle.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.dailycandle.R
import com.example.dailycandle.domain.Summary
import com.example.dailycandle.ui.BalanceState
import com.example.dailycandle.ui.chart.BalanceChart
import com.example.dailycandle.ui.components.AmountText
import com.example.dailycandle.ui.components.ChangeDetails
import com.example.dailycandle.ui.components.SectionTitle
import com.example.dailycandle.ui.components.ValueDetail
import com.example.dailycandle.ui.components.displayLocale
import com.example.dailycandle.ui.format.DisplayFormat
import com.example.dailycandle.ui.theme.Space
import java.time.LocalDate

@Composable
fun HomeScreen(state: BalanceState, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val locale = displayLocale()
    LazyColumn(
        modifier.testTag("home_list"), contentPadding = PaddingValues(Space.large),
        verticalArrangement = Arrangement.spacedBy(Space.section),
    ) {
        val summary = state.summary
        if (summary == null) item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.small)) {
                    SectionTitle(stringResource(R.string.latest_balance))
                    AmountText(summary.latest.value, Modifier.fillMaxWidth().testTag("latest_value"), style = MaterialTheme.typography.displaySmall)
                    Text(stringResource(R.string.recorded_on, DisplayFormat.date(summary.latest.date, locale)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (summary.latestChange != null) {
                        Text(stringResource(R.string.latest_change), style = MaterialTheme.typography.labelMedium)
                        ChangeDetails(summary.latestChange)
                    }
                }
            }
            if (state.candles.isNotEmpty()) {
                item { HorizontalDivider() }
                item { BalanceChart(state.candles, state.selectedDate, onSelect) }
            } else item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.small)) {
                    SectionTitle(stringResource(R.string.first_title))
                    Text(stringResource(R.string.first_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SummarySection(summary) }
        }
        item { Text(stringResource(R.string.chart_explanation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SummarySection(summary: Summary) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val locale = displayLocale()
    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(Space.medium)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("toggle_summary")) {
            Text(stringResource(if (expanded) R.string.hide_summary else R.string.show_summary))
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.large)) {
                SectionTitle(stringResource(R.string.summary_title))
                summary.totalChange?.let { ValueDetail(stringResource(R.string.total_change), it.absolute, signed = true) }
                summary.sevenDayChange?.let { ValueDetail(stringResource(R.string.seven_day_change), it.absolute, signed = true) }
                summary.thirtyDayChange?.let { ValueDetail(stringResource(R.string.thirty_day_change), it.absolute, signed = true) }
                ValueDetail(stringResource(R.string.highest_value), summary.highest.value, caption = DisplayFormat.date(summary.highest.date, locale))
                ValueDetail(stringResource(R.string.lowest_value), summary.lowest.value, caption = DisplayFormat.date(summary.lowest.date, locale))
                Text(stringResource(R.string.summary_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
