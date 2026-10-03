package com.example.dailycandle.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailycandle.data.ThemePreference

object Space {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val section = 24.dp
    val generous = 32.dp
}

data class ChangeColors(val increase: Color, val decrease: Color, val unchanged: Color)
val LocalChangeColors = staticCompositionLocalOf {
    ChangeColors(Color(0xFF12694F), Color(0xFFAC3C29), Color(0xFF515F58))
}

private val Light = lightColorScheme(
    primary = Color(0xFF176B59), onPrimary = Color.White,
    primaryContainer = Color(0xFFCEEEE0), onPrimaryContainer = Color(0xFF073A2C),
    secondary = Color(0xFF496457), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E8DF), onSecondaryContainer = Color(0xFF203C2F),
    background = Color(0xFFF8FAF7), onBackground = Color(0xFF18251F),
    surface = Color(0xFFF8FAF7), onSurface = Color(0xFF18251F),
    surfaceVariant = Color(0xFFE2E9E2), onSurfaceVariant = Color(0xFF485D52),
    surfaceContainer = Color(0xFFEEF2EC), surfaceContainerLow = Color(0xFFF3F6F0),
    surfaceContainerHigh = Color(0xFFE6EDE5),
    outline = Color(0xFF6E8074), outlineVariant = Color(0xFFC2CEC3),
    error = Color(0xFFAC3120), onError = Color.White,
)
private val Dark = darkColorScheme(
    primary = Color(0xFF91D7C0), onPrimary = Color(0xFF063A2B),
    primaryContainer = Color(0xFF184F3F), onPrimaryContainer = Color(0xFFC1EEDC),
    secondary = Color(0xFFB2CCBC), onSecondary = Color(0xFF20392C),
    secondaryContainer = Color(0xFF344F40), onSecondaryContainer = Color(0xFFD0E5D7),
    background = Color(0xFF101714), onBackground = Color(0xFFE4EEE6),
    surface = Color(0xFF101714), onSurface = Color(0xFFE4EEE6),
    surfaceVariant = Color(0xFF34453A), onSurfaceVariant = Color(0xFFB7C8BB),
    surfaceContainer = Color(0xFF1B261F), surfaceContainerLow = Color(0xFF17211A),
    surfaceContainerHigh = Color(0xFF26322A),
    outline = Color(0xFF8A9E8F), outlineVariant = Color(0xFF405347),
    error = Color(0xFFFFB4A5), onError = Color(0xFF651506),
)
private val BaseTypography = Typography()
private val BalanceTypography = Typography(
    displaySmall = BaseTypography.displaySmall.copy(fontSize = 34.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFeatureSettings = "tnum"),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFeatureSettings = "tnum"),
)

@Composable
fun BalanceTheme(preference: ThemePreference = ThemePreference.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
    val view = LocalView.current
    DisposableEffect(view, dark) {
        view.context.activity()?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(0xE6FFFFFF.toInt(), 0x801B1B1B.toInt()) { dark },
        )
        onDispose { }
    }
    CompositionLocalProvider(LocalChangeColors provides if (dark)
        ChangeColors(Color(0xFF81D7B5), Color(0xFFFFB39C), Color(0xFFB4C4BB))
    else ChangeColors(Color(0xFF12694F), Color(0xFFAC3C29), Color(0xFF515F58))) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = BalanceTypography,
            shapes = Shapes(
                small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}

private tailrec fun Context.activity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
