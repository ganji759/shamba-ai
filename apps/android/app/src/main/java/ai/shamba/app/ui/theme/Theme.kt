package ai.shamba.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Design tokens. Keep these in sync with the web app. */
object Tokens {
    val Primary = Color(0xFF1F6B3A)
    val PrimaryDark = Color(0xFF0F3D22)
    val Soil = Color(0xFF7A4E2D)
    val Sun = Color(0xFFF2B705)
    val Background = Color(0xFFF1F5EF)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1B1F1A)
    val Muted = Color(0xFF5D665C)
    val Critical = Color(0xFFB3261E)
    val High = Color(0xFFC2410C)
    val Medium = Color(0xFFA16207)
    val Healthy = Color(0xFF2E7D32)

    val DarkBackground = Color(0xFF101611)
    val DarkSurface = Color(0xFF18211A)
    val DarkInk = Color(0xFFE8EDE6)
    val DarkPrimary = Color(0xFF6FCF8E)
}

/** Colors outside the Material scheme: the one primary action and the urgency badges. */
@Immutable
data class ShambaColors(
    val primaryAction: Color,
    val onPrimaryAction: Color,
    val critical: Color,
    val high: Color,
    val medium: Color,
    val healthy: Color,
    val onBadge: Color,
    val soil: Color,
    val muted: Color,
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Tokens.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E8D8),
    onPrimaryContainer = Tokens.PrimaryDark,
    secondary = Tokens.Soil,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE3D6),
    onSecondaryContainer = Color(0xFF3B2412),
    tertiary = Tokens.Sun,
    onTertiary = Tokens.Ink,
    background = Tokens.Background,
    onBackground = Tokens.Ink,
    surface = Tokens.Surface,
    onSurface = Tokens.Ink,
    surfaceVariant = Color(0xFFE3EAE1),
    onSurfaceVariant = Tokens.Muted,
    surfaceContainerLowest = Tokens.Surface,
    surfaceContainerLow = Tokens.Surface,
    surfaceContainer = Tokens.Surface,
    surfaceContainerHigh = Color(0xFFF7F9F6),
    surfaceContainerHighest = Color(0xFFE9EFE7),
    outline = Color(0xFF8A9488),
    outlineVariant = Color(0xFFC9D2C7),
    error = Tokens.Critical,
    onError = Color.White,
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Tokens.DarkPrimary,
    onPrimary = Tokens.PrimaryDark,
    primaryContainer = Color(0xFF1F4A2E),
    onPrimaryContainer = Color(0xFFCDEFD6),
    secondary = Color(0xFFD9B08C),
    onSecondary = Color(0xFF3B2412),
    secondaryContainer = Color(0xFF4A3220),
    onSecondaryContainer = Color(0xFFF3DFCC),
    tertiary = Tokens.Sun,
    onTertiary = Tokens.Ink,
    background = Tokens.DarkBackground,
    onBackground = Tokens.DarkInk,
    surface = Tokens.DarkSurface,
    onSurface = Tokens.DarkInk,
    surfaceVariant = Color(0xFF243026),
    onSurfaceVariant = Color(0xFFB8C3B5),
    surfaceContainerLowest = Tokens.DarkBackground,
    surfaceContainerLow = Tokens.DarkSurface,
    surfaceContainer = Tokens.DarkSurface,
    surfaceContainerHigh = Color(0xFF1F2A21),
    surfaceContainerHighest = Color(0xFF263228),
    outline = Color(0xFF7F8A7C),
    outlineVariant = Color(0xFF3A463B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

private val LightExtra = ShambaColors(
    primaryAction = Tokens.Sun,
    onPrimaryAction = Tokens.Ink,
    critical = Tokens.Critical,
    high = Tokens.High,
    medium = Tokens.Medium,
    healthy = Tokens.Healthy,
    onBadge = Color.White,
    soil = Tokens.Soil,
    muted = Tokens.Muted,
)

// Badges keep their solid token fill in dark mode. White text on each fill passes WCAG AA.
private val DarkExtra = LightExtra.copy(muted = Color(0xFFB8C3B5), soil = Color(0xFFD9B08C))

val LocalShambaColors = staticCompositionLocalOf { LightExtra }

/** Large type for outdoor reading and low literacy. Body text starts at 18 sp. */
private val ShambaTypography: Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 19.sp, lineHeight = 28.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 17.sp, lineHeight = 25.sp),
        bodySmall = base.bodySmall.copy(fontSize = 15.sp, lineHeight = 21.sp),
        labelLarge = base.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    )
}

val MonoStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp)

@Composable
fun ShambaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalShambaColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = ShambaTypography,
            content = content,
        )
    }
}

object ShambaTheme {
    val colors: ShambaColors
        @Composable get() = LocalShambaColors.current
}
