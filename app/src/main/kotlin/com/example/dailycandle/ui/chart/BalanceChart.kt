package com.example.dailycandle.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailycandle.R
import com.example.dailycandle.domain.Candle
import com.example.dailycandle.domain.ChartViewport
import com.example.dailycandle.domain.Direction
import com.example.dailycandle.domain.ValueRange
import com.example.dailycandle.ui.components.AppIcon
import com.example.dailycandle.ui.components.ChangeDetails
import com.example.dailycandle.ui.components.SectionTitle
import com.example.dailycandle.ui.components.ValueDetail
import com.example.dailycandle.ui.components.displayLocale
import com.example.dailycandle.ui.format.DisplayFormat
import com.example.dailycandle.ui.theme.LocalChangeColors
import com.example.dailycandle.ui.theme.Space
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun BalanceChart(candles: List<Candle>, selectedDate: LocalDate?, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    if (candles.isEmpty()) return
    val initial = ChartViewport.latest(candles.size)
    var start by rememberSaveable { mutableDoubleStateOf(initial.start) }
    var count by rememberSaveable { mutableDoubleStateOf(initial.count) }
    var knownSize by rememberSaveable { mutableIntStateOf(candles.size) }
    var focusedSelection by rememberSaveable { mutableStateOf<Long?>(null) }
    var pixelWidth by remember { mutableIntStateOf(0) }
    val selected = remember(candles, selectedDate) {
        selectedDate?.let { candles.binarySearchBy(it) { candle -> candle.date } }
            ?.takeIf { it >= 0 } ?: if (selectedDate == candles.first().previousDate) 0 else candles.lastIndex
    }
    val locale = displayLocale()
    val density = LocalDensity.current
    val colors = LocalChangeColors.current
    val scheme = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val viewport = ChartViewport(start, count).bounded(candles.size)
    val first = floor(viewport.start).toInt().coerceAtLeast(0)
    val last = ceil(viewport.start + viewport.count).toInt().coerceAtMost(candles.lastIndex)
    val range = remember(candles, first, last) {
        ValueRange.padded(candles.subList(first, last + 1).flatMap { listOf(it.open, it.close) })
    }
    val (axisBase, labels) = remember(range, candles[first].open, locale) {
        val span = range.high.subtract(range.low)
        val step = span.divide(BigDecimal(4))
        val magnitude = maxOf(range.low.abs(), range.high.abs())
        var base = if ((magnitude >= BigDecimal("1000000") || magnitude < BigDecimal("0.001")) &&
            magnitude > span.multiply(BigDecimal.TEN)) candles[first].open else null
        fun labelsFrom(offset: BigDecimal?) = (0..4).map { tick ->
            DisplayFormat.axis(range.at(BigDecimal(tick).divide(BigDecimal(4))).subtract(offset ?: BigDecimal.ZERO), locale, step)
        }
        var ticks = labelsFrom(base)
        // Very small changes around an ordinary-sized balance need an offset too.
        if (ticks.distinct().size < ticks.size && base == null) {
            base = candles[first].open
            ticks = labelsFrom(base)
        }
        base to ticks
    }
    val measuredLabels = labels.map { measurer.measure(it, textStyle) }
    val axisWidth = measuredLabels.maxOf { it.size.width }.toFloat() + with(density) { 16.dp.toPx() }
    val plotWidth = (pixelWidth - axisWidth).coerceAtLeast(1f)
    val currentPlotWidth by rememberUpdatedState(plotWidth)
    fun reset() {
        val latest = ChartViewport.latest(candles.size)
        start = latest.start; count = latest.count
        onSelect(candles.last().date)
    }
    LaunchedEffect(candles.size) {
        if (knownSize != candles.size) {
            val latest = ChartViewport.latest(candles.size)
            start = latest.start; count = latest.count
            knownSize = candles.size
        }
    }
    LaunchedEffect(selectedDate, candles.size) {
        val epochDay = candles[selected].date.toEpochDay()
        // Recomposition/restoration must not pull a deliberately panned view back to selection.
        if (focusedSelection != epochDay) {
            val current = ChartViewport(start, count).bounded(candles.size)
            if (selected < current.start || selected >= current.start + current.count) {
                start = ChartViewport(selected - current.count / 2, current.count).bounded(candles.size).start
            }
            focusedSelection = epochDay
        }
    }
    val resetLabel = stringResource(R.string.reset_chart)
    val description = stringResource(R.string.chart_accessibility)
    val windowDescription = stringResource(R.string.chart_window, first + 1, last + 1, candles.size)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.small)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.chart_title), Modifier.weight(1f))
            IconButton(onClick = ::reset, modifier = Modifier.testTag("reset_chart")) { AppIcon(R.drawable.ic_reset, description = resetLabel) }
        }
        if (axisBase != null) Text(
            stringResource(R.string.axis_relative, DisplayFormat.value(axisBase, locale)),
            Modifier.horizontalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall,
        )
        Canvas(
            Modifier.fillMaxWidth().height(260.dp).testTag("balance_chart")
                .onSizeChanged { pixelWidth = it.width }
                .semantics {
                    contentDescription = description
                    stateDescription = windowDescription
                    customActions = listOf(CustomAccessibilityAction(resetLabel) { reset(); true })
                }
                .pointerInput(candles) {
                    detectTapGestures(
                        onDoubleTap = { reset() },
                        onTap = { offset ->
                            if (offset.x <= currentPlotWidth) {
                                val current = ChartViewport(start, count).bounded(candles.size)
                                val index = floor(current.start + offset.x / currentPlotWidth * current.count).toInt().coerceIn(0, candles.lastIndex)
                                onSelect(candles[index].date)
                            }
                        },
                    )
                }
                .pointerInput(candles.size) {
                    detectChartTransforms { center, pan, zoom ->
                        val next = ChartViewport(start, count).transform(
                            candles.size, zoom.toDouble(), (pan.x / currentPlotWidth).toDouble(), (center.x / currentPlotWidth).toDouble(),
                        )
                        start = next.start; count = next.count
                    }
                },
        ) {
            val top = 12.dp.toPx()
            val bottom = size.height - 36.sp.toPx()
            val plotHeight = (bottom - top).coerceAtLeast(1f)
            val plot = (size.width - axisWidth).coerceAtLeast(1f)
            val gap = plot / viewport.count.toFloat()
            fun x(index: Int) = ((index - viewport.start + 0.5) / viewport.count * plot).toFloat()
            fun y(value: BigDecimal) = bottom - range.fraction(value) * plotHeight
            for (tick in 0..4) {
                val y = bottom - plotHeight * tick / 4f
                drawLine(scheme.outlineVariant.copy(alpha = 0.6f), Offset(0f, y), Offset(plot, y), 1.dp.toPx())
                val label = measuredLabels[tick]
                drawText(label, topLeft = Offset(plot + 8.dp.toPx(), (y - label.size.height / 2).coerceAtLeast(0f)))
            }
            val bodyWidth = (gap * 0.56f).coerceIn(1f, 32.dp.toPx())
            for (index in first..last) {
                val candle = candles[index]
                val centerX = x(index)
                if (centerX < 0 || centerX > plot) continue
                val open = y(candle.open)
                val close = y(candle.close)
                val bodyTop = minOf(open, close)
                val bodyHeight = maxOf(kotlin.math.abs(close - open), 2.dp.toPx())
                val color = when (candle.change.direction) {
                    Direction.INCREASE -> colors.increase
                    Direction.DECREASE -> colors.decrease
                    Direction.UNCHANGED -> colors.unchanged
                }
                if (index == selected) {
                    drawRect(scheme.primary.copy(alpha = 0.08f), Offset(centerX - gap / 2, top), Size(gap, plotHeight))
                    drawLine(scheme.onSurface.copy(alpha = 0.6f), Offset(centerX, top), Offset(centerX, bottom), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                }
                // Body only. Unchanged values are horizontal marks, never invented shadows.
                if (candle.change.direction == Direction.UNCHANGED) {
                    drawLine(color, Offset(centerX - bodyWidth / 2, close), Offset(centerX + bodyWidth / 2, close), 3.dp.toPx())
                } else {
                    drawRect(color, Offset(centerX - bodyWidth / 2, bodyTop), Size(bodyWidth, bodyHeight))
                }
                if (index == selected) drawRect(
                    scheme.onSurface, Offset(centerX - bodyWidth / 2 - 3.dp.toPx(), bodyTop - 3.dp.toPx()),
                    Size(bodyWidth + 6.dp.toPx(), bodyHeight + 6.dp.toPx()), style = Stroke(1.dp.toPx()),
                )
            }
            val step = ceil(90.sp.toPx() / gap).toInt().coerceAtLeast(1)
            val wide = candles[last].date.toEpochDay() - candles[first].previousDate.toEpochDay() > 90 && viewport.count > 32
            for (index in first..last) {
                if (index % step != 0) continue
                val centerX = x(index)
                if (centerX !in 0f..plot) continue
                val label = measurer.measure(DisplayFormat.shortDate(candles[index].date, locale, wide), textStyle)
                drawText(label, topLeft = Offset((centerX - label.size.width / 2).coerceIn(0f, (plot - label.size.width).coerceAtLeast(0f)), bottom + 12.dp.toPx()))
            }
        }
        Text(stringResource(R.string.chart_help), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        SelectionPanel(candles[selected], selected > 0, selected < candles.lastIndex,
            onPrevious = { onSelect(candles[selected - 1].date) }, onNext = { onSelect(candles[selected + 1].date) })
    }
}

@Composable
private fun SelectionPanel(candle: Candle, previous: Boolean, next: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val locale = displayLocale()
    Column(Modifier.fillMaxWidth().testTag("candle_details"), verticalArrangement = Arrangement.spacedBy(Space.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(DisplayFormat.date(candle.date, locale), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.since_date, DisplayFormat.date(candle.previousDate, locale)), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPrevious, enabled = previous, modifier = Modifier.testTag("previous_candle")) {
                AppIcon(R.drawable.ic_previous, description = stringResource(R.string.previous_candle))
            }
            IconButton(onClick = onNext, enabled = next, modifier = Modifier.testTag("next_candle")) {
                AppIcon(R.drawable.ic_next, description = stringResource(R.string.next_candle))
            }
        }
        BoxWithConstraints {
            if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.4f) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.medium)) {
                    ValueDetail(stringResource(R.string.open_value), candle.open)
                    ValueDetail(stringResource(R.string.close_value), candle.close)
                }
            } else Row(horizontalArrangement = Arrangement.spacedBy(Space.large)) {
                ValueDetail(stringResource(R.string.open_value), candle.open, Modifier.weight(1f))
                ValueDetail(stringResource(R.string.close_value), candle.close, Modifier.weight(1f))
            }
        }
        ChangeDetails(candle.change)
    }
}
