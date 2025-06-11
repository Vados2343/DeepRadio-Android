package com.myradio.deepradio.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ✅ Enum для типов тем
enum class AppTheme {
    LIGHT,      // Светлая тема
    DARK,       // Темная тема
    AMOLED,     // Чистый черный (AMOLED)
    LIQUID_GLASS // Liquid Glass с прозрачностями
}

// ✅ ИСПРАВЛЕННЫЕ расширения для получения описаний тем
fun AppTheme.getDisplayName(): String = when (this) {
    AppTheme.LIGHT -> "Светлая"
    AppTheme.DARK -> "Темная"
    AppTheme.AMOLED -> "AMOLED"
    AppTheme.LIQUID_GLASS -> "Liquid Glass"
}

fun AppTheme.getDescription(): String = when (this) {
    AppTheme.LIGHT -> "Светлая тема с яркими цветами для дневного использования"
    AppTheme.DARK -> "Темная тема для комфортного использования в темное время"
    AppTheme.AMOLED -> "Чистый черный цвет для AMOLED экранов. Экономит батарею"
    AppTheme.LIQUID_GLASS -> "Современная тема с эффектами стекла и прозрачности"
}

fun AppTheme.getRecommendation(): String = when (this) {
    AppTheme.LIGHT -> "🌞 Рекомендуется для дневного использования"
    AppTheme.DARK -> "🌙 Рекомендуется для вечернего использования"
    AppTheme.AMOLED -> "🔋 Экономит батарею на AMOLED экранах"
    AppTheme.LIQUID_GLASS -> "✨ Современный стиль для любителей инноваций"
}

// ✅ УЛУЧШЕННАЯ светлая тема
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A75),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4DD0E1),
    onPrimaryContainer = Color(0xFF002419),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF80CBC4),
    onSecondaryContainer = Color(0xFF002622),
    tertiary = Color(0xFF795548),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7CCC8),
    onTertiaryContainer = Color(0xFF3E2723),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFECEFF1),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0F2F1),
    onSurfaceVariant = Color(0xFF37474F),
    outline = Color(0xFF607D8B),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF4DD0E1)
)

// ✅ УЛУЧШЕННАЯ темная тема
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),
    onPrimary = Color(0xFF003F47),
    primaryContainer = Color(0xFF00574B),
    onPrimaryContainer = Color(0xFFA7FFEB),
    secondary = Color(0xFF64FFDA),
    onSecondary = Color(0xFF003E2F),
    secondaryContainer = Color(0xFF00574B),
    onSecondaryContainer = Color(0xFFB2FFF0),
    tertiary = Color(0xFFBCAAA4),
    onTertiary = Color(0xFF2D1B16),
    tertiaryContainer = Color(0xFF44312B),
    onTertiaryContainer = Color(0xFFD8C2BC),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1F23),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF78909C),
    inverseOnSurface = Color(0xFF1A1C1E),
    inverseSurface = Color(0xFFE3E2E6),
    inversePrimary = Color(0xFF006A75)
)

// ✅ НОВАЯ AMOLED тема (чистый черный)
private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5B),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFF40E0D0),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003D36),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFF81C784),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFC8E6C9),
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF690005),
    onError = Color.Black,
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,           // Чистый черный
    onBackground = Color(0xFFFFFFFF),   // Чистый белый текст
    surface = Color(0xFF0A0A0A),        // Почти черный для карточек
    onSurface = Color(0xFFFFFFFF),      // Белый текст
    surfaceVariant = Color(0xFF1A1A1A), // Темно-серый для вариантов
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFF404040),
    inverseOnSurface = Color.Black,
    inverseSurface = Color.White,
    inversePrimary = Color(0xFF006A75)
)

// ✅ НОВАЯ Liquid Glass тема (с эффектами стекла)
private val LiquidGlassColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D1B2A),
    primaryContainer = Color(0xFF1565C0).copy(alpha = 0.85f),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF1B2A1B),
    secondaryContainer = Color(0xFF388E3C).copy(alpha = 0.85f),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFBA68C8),
    onTertiary = Color(0xFF2A1B2A),
    tertiaryContainer = Color(0xFF7B1FA2).copy(alpha = 0.85f),
    onTertiaryContainer = Color(0xFFE1BEE7),
    error = Color(0xFFEF5350),
    errorContainer = Color(0xFFD32F2F).copy(alpha = 0.85f),
    onError = Color(0xFF2A1B1B),
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF0A0E1A).copy(alpha = 0.95f),    // Полупрозрачный фон
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF1A237E).copy(alpha = 0.4f),        // Стеклянные поверхности
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF283593).copy(alpha = 0.5f), // Стеклянные варианты
    onSurfaceVariant = Color(0xFFC5CAE9),
    outline = Color(0xFF5C6BC0).copy(alpha = 0.7f),
    inverseOnSurface = Color(0xFF0A0E1A),
    inverseSurface = Color(0xFFECEFF1),
    inversePrimary = Color(0xFF1976D2)
)

@Composable
fun DeepRadioTheme(
    appTheme: AppTheme = AppTheme.DARK,
    dynamicColor: Boolean = false, // Отключаем по умолчанию для наших кастомных тем
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Динамические цвета только для стандартных тем
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (appTheme == AppTheme.LIGHT || appTheme == AppTheme.DARK) -> {
            val context = LocalContext.current
            when (appTheme) {
                AppTheme.DARK -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        // Кастомные темы
        else -> when (appTheme) {
            AppTheme.LIGHT -> LightColorScheme
            AppTheme.DARK -> DarkColorScheme
            AppTheme.AMOLED -> AmoledColorScheme
            AppTheme.LIQUID_GLASS -> LiquidGlassColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // ✅ УЛУЧШЕННАЯ настройка цвета статус бара в зависимости от темы
            window.statusBarColor = when (appTheme) {
                AppTheme.LIGHT -> colorScheme.primary.toArgb()
                AppTheme.DARK -> colorScheme.surface.toArgb()
                AppTheme.AMOLED -> Color.Black.toArgb()
                AppTheme.LIQUID_GLASS -> Color.Transparent.toArgb()
            }

            // ✅ УЛУЧШЕННАЯ настройка цвета навигационной панели
            window.navigationBarColor = when (appTheme) {
                AppTheme.LIGHT -> colorScheme.surface.toArgb()
                AppTheme.DARK -> colorScheme.surface.toArgb()
                AppTheme.AMOLED -> Color.Black.toArgb()
                AppTheme.LIQUID_GLASS -> Color.Transparent.toArgb()
            }

            // ✅ УЛУЧШЕННАЯ настройка светлых/темных иконок статус бара
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                appTheme == AppTheme.LIGHT

            // ✅ УЛУЧШЕННАЯ настройка светлых/темных иконок навигации
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                appTheme == AppTheme.LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EnhancedTypography,
        content = content
    )
}

// ✅ УЛУЧШЕННАЯ типография
val EnhancedTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold, // ✅ Изменено на Bold
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // ✅ Изменено на SemiBold
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // ✅ Изменено на SemiBold
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium, // ✅ Изменено на Medium
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ✅ НОВЫЕ ДОПОЛНИТЕЛЬНЫЕ ФУНКЦИИ
object ThemeHelper {
    fun isDarkTheme(theme: AppTheme): Boolean = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED, AppTheme.LIQUID_GLASS -> true
    }

    fun isTransparentTheme(theme: AppTheme): Boolean = when (theme) {
        AppTheme.LIQUID_GLASS -> true
        else -> false
    }

    fun getThemeForTime(): AppTheme {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..18 -> AppTheme.LIGHT    // День
            in 19..22 -> AppTheme.DARK    // Вечер
            else -> AppTheme.AMOLED       // Ночь
        }
    }
}