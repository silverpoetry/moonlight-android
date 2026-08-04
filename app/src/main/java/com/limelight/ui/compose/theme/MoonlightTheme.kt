package com.limelight.ui.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.limelight.settings.android.AndroidAppPresentationSettingsLoader
import com.limelight.settings.app.AppPresentationSettingKeys

private val MoonlightLightColors = lightColorScheme(
    primary = Color(0xFF5C5FDE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E0FF),
    onPrimaryContainer = Color(0xFF15164F),
    secondary = Color(0xFF5E5D72),
    secondaryContainer = Color(0xFFE4E1F3),
    onSecondaryContainer = Color(0xFF1B1A2C),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE6E1EC),
    onSurfaceVariant = Color(0xFF48454E),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val MoonlightDarkColors = darkColorScheme(
    primary = Color(0xFFC2C1FF),
    onPrimary = Color(0xFF292A8A),
    primaryContainer = Color(0xFF4143A5),
    onPrimaryContainer = Color(0xFFE2E0FF),
    secondary = Color(0xFFC7C4DC),
    secondaryContainer = Color(0xFF464559),
    onSecondaryContainer = Color(0xFFE4E1F3),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

private val MoonlightTypography
    @Composable get() = MaterialTheme.typography.copy(
        displaySmall = TextStyle(
            fontSize = 36.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Normal,
        ),
        headlineLarge = TextStyle(
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Normal,
        ),
        titleLarge = TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        ),
    )

/**
 * Moonlight's sole Material 3 theme boundary.
 *
 * Feature screens must consume [MaterialTheme] values instead of declaring
 * colors, shapes, or typography locally. This keeps dynamic color and future
 * Material upgrades independent from feature code.
 */
@Composable
fun MoonlightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = remember(context, darkTheme, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }
            darkTheme -> MoonlightDarkColors
            else -> MoonlightLightColors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoonlightTypography,
        content = content,
    )
}

/** Applies the existing app appearance preference to Compose screens. */
@Composable
fun MoonlightThemeFromSettings(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeMode = remember(context) {
        AndroidAppPresentationSettingsLoader.load(context).themeMode
    }
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppPresentationSettingKeys.THEME_MODE_LIGHT -> false
        AppPresentationSettingKeys.THEME_MODE_DARK -> true
        else -> systemDarkTheme
    }
    MoonlightTheme(
        darkTheme = darkTheme,
        content = content,
    )
}
