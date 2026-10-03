package com.example.dailycandle.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.dailycandle.R
import com.example.dailycandle.domain.Change
import com.example.dailycandle.domain.Direction
import com.example.dailycandle.ui.format.DisplayFormat
import com.example.dailycandle.ui.theme.LocalChangeColors
import com.example.dailycandle.ui.theme.Space
import java.math.BigDecimal
import java.util.Locale

@Composable fun displayLocale(): Locale = LocalLocale.current.platformLocale

@Composable
fun AppIcon(resource: Int, modifier: Modifier = Modifier, description: String? = null) {
    Icon(painterResource(resource), description, modifier.size(22.dp))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@Composable
fun AmountText(
    value: BigDecimal,
    modifier: Modifier = Modifier,
    signed: Boolean = false,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val locale = displayLocale()
    SelectionContainer {
        Text(
            if (signed) DisplayFormat.signed(value, locale) else DisplayFormat.value(value, locale),
            modifier.horizontalScroll(rememberScrollState()),
            softWrap = false, style = style, color = color,
        )
    }
}

@Composable
fun changeColor(direction: Direction): Color = when (direction) {
    Direction.INCREASE -> LocalChangeColors.current.increase
    Direction.DECREASE -> LocalChangeColors.current.decrease
    Direction.UNCHANGED -> LocalChangeColors.current.unchanged
}

@Composable
fun directionLabel(direction: Direction): String = stringResource(when (direction) {
    Direction.INCREASE -> R.string.increase
    Direction.DECREASE -> R.string.decrease
    Direction.UNCHANGED -> R.string.unchanged
})

@Composable
fun ChangeDetails(change: Change, modifier: Modifier = Modifier) {
    val color = changeColor(change.direction)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.tiny)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.small), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(when (change.direction) {
                Direction.INCREASE -> R.drawable.ic_increase
                Direction.DECREASE -> R.drawable.ic_decrease
                Direction.UNCHANGED -> R.drawable.ic_unchanged
            }), null, Modifier.size(20.dp), tint = color)
            Text(directionLabel(change.direction), style = MaterialTheme.typography.labelLarge, color = color)
        }
        AmountText(change.absolute, Modifier.fillMaxWidth(), signed = true, color = color)
        if (change.percent != null) {
            Text(DisplayFormat.percent(change.percent, displayLocale()), Modifier.horizontalScroll(rememberScrollState()),
                softWrap = false, color = color, style = MaterialTheme.typography.bodyMedium)
        } else Text(stringResource(R.string.percentage_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ValueDetail(label: String, value: BigDecimal, modifier: Modifier = Modifier, caption: String? = null, signed: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.tiny)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(value, Modifier.fillMaxWidth(), signed = signed)
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
