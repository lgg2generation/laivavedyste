package lt.laivavedys.testai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// A deep maritime blue with a warm buoy-orange accent.
private val Navy = Color(0xFF0B4F86)
private val NavyLight = Color(0xFFCFE4F7)
private val Buoy = Color(0xFFE07A2F)

val StatusGreen = Color(0xFF2E9E5B)
val StatusYellow = Color(0xFFE0A32F)
val StatusRed = Color(0xFFD24B3E)
val StatusNew = Color(0xFF9AA5B1)

val StatusGreenDark = Color(0xFF57C888)
val StatusYellowDark = Color(0xFFF0C462)
val StatusRedDark = Color(0xFFF07A6E)
val StatusNewDark = Color(0xFF6B7683)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color(0xFF07304F),
    secondary = Color(0xFF4A6072),
    tertiary = Buoy,
    onTertiary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF11181F),
    surface = Color.White,
    onSurface = Color(0xFF11181F),
    surfaceVariant = Color(0xFFE6ECF3),
    onSurfaceVariant = Color(0xFF44525E),
    outline = Color(0xFFA9B6C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FC4EE),
    onPrimary = Color(0xFF03293F),
    primaryContainer = Color(0xFF0F3C5F),
    onPrimaryContainer = NavyLight,
    secondary = Color(0xFFB2C4D4),
    tertiary = Color(0xFFF2A464),
    onTertiary = Color(0xFF3D1E05),
    background = Color(0xFF0E1419),
    onBackground = Color(0xFFE2E8EE),
    surface = Color(0xFF161E25),
    onSurface = Color(0xFFE2E8EE),
    surfaceVariant = Color(0xFF2A343D),
    onSurfaceVariant = Color(0xFFBCC7D2),
    outline = Color(0xFF5A6873),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun LaivavedysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}

/** Traffic-light colours that stay readable in both themes. */
@Composable
fun statusColor(status: lt.laivavedys.testai.data.Status): Color {
    val dark = isSystemInDarkTheme()
    return when (status) {
        lt.laivavedys.testai.data.Status.GREEN -> if (dark) StatusGreenDark else StatusGreen
        lt.laivavedys.testai.data.Status.YELLOW -> if (dark) StatusYellowDark else StatusYellow
        lt.laivavedys.testai.data.Status.RED -> if (dark) StatusRedDark else StatusRed
        lt.laivavedys.testai.data.Status.NEW -> if (dark) StatusNewDark else StatusNew
    }
}
