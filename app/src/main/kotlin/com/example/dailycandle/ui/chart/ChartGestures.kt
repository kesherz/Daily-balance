package com.example.dailycandle.ui.chart

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/** Lock single-finger pans horizontally so vertical scrolling still belongs to the page. */
suspend fun PointerInputScope.detectChartTransforms(onTransform: (Offset, Offset, Float) -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var passedSlop = false
        var accumulatedPan = Offset.Zero
        var accumulatedZoom = 1f
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) break
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            accumulatedPan += pan
            accumulatedZoom *= zoom
            if (!passedSlop) {
                val multiple = event.changes.count { it.pressed } > 1
                val zoomMotion = abs(1 - accumulatedZoom) * event.calculateCentroidSize(useCurrent = false)
                val horizontal = abs(accumulatedPan.x)
                val vertical = abs(accumulatedPan.y)
                if (!multiple && vertical > viewConfiguration.touchSlop && vertical > horizontal) break
                passedSlop = zoomMotion > viewConfiguration.touchSlop ||
                    (horizontal > viewConfiguration.touchSlop && (multiple || horizontal > vertical))
            }
            if (passedSlop) {
                // A final pointer-up has no centroid. Never feed it into viewport arithmetic.
                if (pan != Offset.Zero || zoom != 1f) {
                    val center = event.calculateCentroid(useCurrent = false)
                    if (center.x.isFinite() && center.y.isFinite() && zoom.isFinite()) {
                        onTransform(center, pan, zoom)
                    }
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}
