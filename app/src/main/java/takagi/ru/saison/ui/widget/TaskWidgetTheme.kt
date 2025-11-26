package takagi.ru.saison.ui.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders

/**
 * 任务小组件主题配置
 */
object TaskWidgetTheme {
    // 品牌色
    val BrandBlue = Color(0xFF4A90E2)
    val DarkBackground = Color(0xFF121212)
    val CardSurface = Color(0xFF2C2C2C)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF999999)
    val EmptyStateGray = Color(0xFF444444)
    
    // Glance ColorProviders
    val colors = ColorProviders(
        light = lightColorScheme(
            primary = BrandBlue,
            surface = Color.White,
            onSurface = Color.Black,
            background = Color.White,
            onBackground = Color.Black
        ),
        dark = darkColorScheme(
            primary = BrandBlue,
            surface = DarkBackground,
            onSurface = TextPrimary,
            background = DarkBackground,
            onBackground = TextPrimary
        )
    )
}
