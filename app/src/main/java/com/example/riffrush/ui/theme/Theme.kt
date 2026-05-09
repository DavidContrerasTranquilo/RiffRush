package com.example.riffrush.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun RiffRushTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

data class AppThemeColors(
    val background: Brush,
    val topBarText: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textMain: Color,
    val textMuted: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val vinylBg: Brush
)

@Composable
fun getAppThemeColors(): AppThemeColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return AppThemeColors(
        background = if (isDark) Brush.verticalGradient(listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))) 
                     else Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFCBD5E1))),
        topBarText = if (isDark) Color.White else Color(0xFF1E293B),
        cardBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.6f),
        cardBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
        textMain = if (isDark) Color.White else Color(0xFF0F172A),
        textMuted = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569),
        accentPrimary = if (isDark) Color(0xFF00FFCC) else Color(0xFF0EA5E9),
        accentSecondary = if (isDark) Color(0xFFFF007A) else Color(0xFFE11D48),
        buttonBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White,
        buttonText = if (isDark) Color.White else Color(0xFF1E293B),
        vinylBg = if (isDark) Brush.radialGradient(listOf(Color(0xFF2C2C3E), Color(0xFF12121B)))
                  else Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)))
    )
}
