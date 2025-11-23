package takagi.ru.saison.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import takagi.ru.saison.data.local.datastore.SeasonalTheme

@Composable
fun SaisonTheme(
    seasonalTheme: SeasonalTheme = SeasonalTheme.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && seasonalTheme == SeasonalTheme.DYNAMIC -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getSeasonalColorScheme(seasonalTheme, darkTheme)
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun getSeasonalColorScheme(theme: SeasonalTheme, darkTheme: Boolean): ColorScheme {
    // 如果是自动季节模式，根据当前日期选择主题
    val actualTheme = if (theme == SeasonalTheme.AUTO_SEASONAL) {
        takagi.ru.saison.util.SeasonHelper.getCurrentSeasonTheme()
    } else {
        theme
    }
    
    return when (actualTheme) {
        SeasonalTheme.SAKURA -> if (darkTheme) SakuraDarkScheme else SakuraLightScheme
        SeasonalTheme.MINT -> if (darkTheme) MintDarkScheme else MintLightScheme
        SeasonalTheme.AMBER -> if (darkTheme) AmberDarkScheme else AmberLightScheme
        SeasonalTheme.SNOW -> if (darkTheme) SnowDarkScheme else SnowLightScheme
        SeasonalTheme.RAIN -> if (darkTheme) RainDarkScheme else RainLightScheme
        SeasonalTheme.MAPLE -> if (darkTheme) MapleDarkScheme else MapleLightScheme
        SeasonalTheme.OCEAN -> if (darkTheme) OceanDarkScheme else OceanLightScheme
        SeasonalTheme.SUNSET -> if (darkTheme) SunsetDarkScheme else SunsetLightScheme
        SeasonalTheme.FOREST -> if (darkTheme) ForestDarkScheme else ForestLightScheme
        SeasonalTheme.LAVENDER -> if (darkTheme) LavenderDarkScheme else LavenderLightScheme
        SeasonalTheme.DESERT -> if (darkTheme) DesertDarkScheme else DesertLightScheme
        SeasonalTheme.AURORA -> if (darkTheme) AuroraDarkScheme else AuroraLightScheme
        SeasonalTheme.AUTO_SEASONAL -> if (darkTheme) SakuraDarkScheme else SakuraLightScheme // 不应该到这里
        SeasonalTheme.DYNAMIC -> if (darkTheme) darkColorScheme() else lightColorScheme()
    }
}

// Sakura Color Schemes
private val SakuraLightScheme = lightColorScheme(
    primary = SakuraPrimary,
    onPrimary = SakuraOnPrimary,
    primaryContainer = SakuraPrimaryContainer,
    onPrimaryContainer = SakuraOnPrimaryContainer,
    secondary = SakuraSecondary,
    onSecondary = SakuraOnSecondary,
    secondaryContainer = SakuraSecondaryContainer,
    onSecondaryContainer = SakuraOnSecondaryContainer,
    tertiary = SakuraTertiary,
    onTertiary = SakuraOnTertiary,
    tertiaryContainer = SakuraTertiaryContainer,
    onTertiaryContainer = SakuraOnTertiaryContainer,
    error = SakuraError,
    onError = SakuraOnError,
    errorContainer = SakuraErrorContainer,
    onErrorContainer = SakuraOnErrorContainer,
    background = SakuraBackground,
    onBackground = SakuraOnBackground,
    surface = SakuraSurface,
    onSurface = SakuraOnSurface,
    surfaceVariant = SakuraSurfaceVariant,
    onSurfaceVariant = SakuraOnSurfaceVariant,
    outline = SakuraOutline,
    outlineVariant = SakuraOutlineVariant
)

private val SakuraDarkScheme = darkColorScheme(
    primary = SakuraDarkPrimary,
    onPrimary = SakuraDarkOnPrimary,
    primaryContainer = SakuraDarkPrimaryContainer,
    onPrimaryContainer = SakuraDarkOnPrimaryContainer,
    secondary = SakuraDarkSecondary,
    onSecondary = SakuraDarkOnSecondary,
    secondaryContainer = SakuraDarkSecondaryContainer,
    onSecondaryContainer = SakuraDarkOnSecondaryContainer,
    tertiary = SakuraDarkTertiary,
    onTertiary = SakuraDarkOnTertiary,
    tertiaryContainer = SakuraDarkTertiaryContainer,
    onTertiaryContainer = SakuraDarkOnTertiaryContainer,
    error = SakuraDarkError,
    onError = SakuraDarkOnError,
    errorContainer = SakuraDarkErrorContainer,
    onErrorContainer = SakuraDarkOnErrorContainer,
    background = SakuraDarkBackground,
    onBackground = SakuraDarkOnBackground,
    surface = SakuraDarkSurface,
    onSurface = SakuraDarkOnSurface,
    surfaceVariant = SakuraDarkSurfaceVariant,
    onSurfaceVariant = SakuraDarkOnSurfaceVariant,
    outline = SakuraDarkOutline,
    outlineVariant = SakuraDarkOutlineVariant
)

// Mint Color Schemes
private val MintLightScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = MintOnSecondary,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = MintOnSecondaryContainer,
    tertiary = MintTertiary,
    onTertiary = MintOnTertiary,
    tertiaryContainer = MintTertiaryContainer,
    onTertiaryContainer = MintOnTertiaryContainer,
    error = MintError,
    onError = MintOnError,
    errorContainer = MintErrorContainer,
    onErrorContainer = MintOnErrorContainer,
    background = MintBackground,
    onBackground = MintOnBackground,
    surface = MintSurface,
    onSurface = MintOnSurface,
    surfaceVariant = MintSurfaceVariant,
    onSurfaceVariant = MintOnSurfaceVariant,
    outline = MintOutline,
    outlineVariant = MintOutlineVariant
)

private val MintDarkScheme = darkColorScheme(
    primary = MintDarkPrimary,
    onPrimary = MintDarkOnPrimary,
    primaryContainer = MintDarkPrimaryContainer,
    onPrimaryContainer = MintDarkOnPrimaryContainer,
    secondary = MintDarkSecondary,
    onSecondary = MintDarkOnSecondary,
    secondaryContainer = MintDarkSecondaryContainer,
    onSecondaryContainer = MintDarkOnSecondaryContainer,
    tertiary = MintDarkTertiary,
    onTertiary = MintDarkOnTertiary,
    tertiaryContainer = MintDarkTertiaryContainer,
    onTertiaryContainer = MintDarkOnTertiaryContainer,
    error = MintDarkError,
    onError = MintDarkOnError,
    errorContainer = MintDarkErrorContainer,
    onErrorContainer = MintDarkOnErrorContainer,
    background = MintDarkBackground,
    onBackground = MintDarkOnBackground,
    surface = MintDarkSurface,
    onSurface = MintDarkOnSurface,
    surfaceVariant = MintDarkSurfaceVariant,
    onSurfaceVariant = MintDarkOnSurfaceVariant,
    outline = MintDarkOutline,
    outlineVariant = MintDarkOutlineVariant
)

// Amber Color Schemes
private val AmberLightScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    error = AmberError,
    onError = AmberOnError,
    errorContainer = AmberErrorContainer,
    onErrorContainer = AmberOnErrorContainer,
    background = AmberBackground,
    onBackground = AmberOnBackground,
    surface = AmberSurface,
    onSurface = AmberOnSurface,
    surfaceVariant = AmberSurfaceVariant,
    onSurfaceVariant = AmberOnSurfaceVariant,
    outline = AmberOutline,
    outlineVariant = AmberOutlineVariant
)

private val AmberDarkScheme = darkColorScheme(
    primary = AmberDarkPrimary,
    onPrimary = AmberDarkOnPrimary,
    primaryContainer = AmberDarkPrimaryContainer,
    onPrimaryContainer = AmberDarkOnPrimaryContainer,
    secondary = AmberDarkSecondary,
    onSecondary = AmberDarkOnSecondary,
    secondaryContainer = AmberDarkSecondaryContainer,
    onSecondaryContainer = AmberDarkOnSecondaryContainer,
    tertiary = AmberDarkTertiary,
    onTertiary = AmberDarkOnTertiary,
    tertiaryContainer = AmberDarkTertiaryContainer,
    onTertiaryContainer = AmberDarkOnTertiaryContainer,
    error = AmberDarkError,
    onError = AmberDarkOnError,
    errorContainer = AmberDarkErrorContainer,
    onErrorContainer = AmberDarkOnErrorContainer,
    background = AmberDarkBackground,
    onBackground = AmberDarkOnBackground,
    surface = AmberDarkSurface,
    onSurface = AmberDarkOnSurface,
    surfaceVariant = AmberDarkSurfaceVariant,
    onSurfaceVariant = AmberDarkOnSurfaceVariant,
    outline = AmberDarkOutline,
    outlineVariant = AmberDarkOutlineVariant
)

// Snow Color Schemes
private val SnowLightScheme = lightColorScheme(
    primary = SnowPrimary,
    onPrimary = SnowOnPrimary,
    primaryContainer = SnowPrimaryContainer,
    onPrimaryContainer = SnowOnPrimaryContainer,
    secondary = SnowSecondary,
    onSecondary = SnowOnSecondary,
    secondaryContainer = SnowSecondaryContainer,
    onSecondaryContainer = SnowOnSecondaryContainer,
    tertiary = SnowTertiary,
    onTertiary = SnowOnTertiary,
    tertiaryContainer = SnowTertiaryContainer,
    onTertiaryContainer = SnowOnTertiaryContainer,
    error = SnowError,
    onError = SnowOnError,
    errorContainer = SnowErrorContainer,
    onErrorContainer = SnowOnErrorContainer,
    background = SnowBackground,
    onBackground = SnowOnBackground,
    surface = SnowSurface,
    onSurface = SnowOnSurface,
    surfaceVariant = SnowSurfaceVariant,
    onSurfaceVariant = SnowOnSurfaceVariant,
    outline = SnowOutline,
    outlineVariant = SnowOutlineVariant
)

private val SnowDarkScheme = darkColorScheme(
    primary = SnowDarkPrimary,
    onPrimary = SnowDarkOnPrimary,
    primaryContainer = SnowDarkPrimaryContainer,
    onPrimaryContainer = SnowDarkOnPrimaryContainer,
    secondary = SnowDarkSecondary,
    onSecondary = SnowDarkOnSecondary,
    secondaryContainer = SnowDarkSecondaryContainer,
    onSecondaryContainer = SnowDarkOnSecondaryContainer,
    tertiary = SnowDarkTertiary,
    onTertiary = SnowDarkOnTertiary,
    tertiaryContainer = SnowDarkTertiaryContainer,
    onTertiaryContainer = SnowDarkOnTertiaryContainer,
    error = SnowDarkError,
    onError = SnowDarkOnError,
    errorContainer = SnowDarkErrorContainer,
    onErrorContainer = SnowDarkOnErrorContainer,
    background = SnowDarkBackground,
    onBackground = SnowDarkOnBackground,
    surface = SnowDarkSurface,
    onSurface = SnowDarkOnSurface,
    surfaceVariant = SnowDarkSurfaceVariant,
    onSurfaceVariant = SnowDarkOnSurfaceVariant,
    outline = SnowDarkOutline,
    outlineVariant = SnowDarkOutlineVariant
)

// Rain Color Schemes
private val RainLightScheme = lightColorScheme(
    primary = RainPrimary,
    onPrimary = RainOnPrimary,
    primaryContainer = RainPrimaryContainer,
    onPrimaryContainer = RainOnPrimaryContainer,
    secondary = RainSecondary,
    onSecondary = RainOnSecondary,
    secondaryContainer = RainSecondaryContainer,
    onSecondaryContainer = RainOnSecondaryContainer,
    tertiary = RainTertiary,
    onTertiary = RainOnTertiary,
    tertiaryContainer = RainTertiaryContainer,
    onTertiaryContainer = RainOnTertiaryContainer,
    error = RainError,
    onError = RainOnError,
    errorContainer = RainErrorContainer,
    onErrorContainer = RainOnErrorContainer,
    background = RainBackground,
    onBackground = RainOnBackground,
    surface = RainSurface,
    onSurface = RainOnSurface,
    surfaceVariant = RainSurfaceVariant,
    onSurfaceVariant = RainOnSurfaceVariant,
    outline = RainOutline,
    outlineVariant = RainOutlineVariant
)

private val RainDarkScheme = darkColorScheme(
    primary = RainDarkPrimary,
    onPrimary = RainDarkOnPrimary,
    primaryContainer = RainDarkPrimaryContainer,
    onPrimaryContainer = RainDarkOnPrimaryContainer,
    secondary = RainDarkSecondary,
    onSecondary = RainDarkOnSecondary,
    secondaryContainer = RainDarkSecondaryContainer,
    onSecondaryContainer = RainDarkOnSecondaryContainer,
    tertiary = RainDarkTertiary,
    onTertiary = RainDarkOnTertiary,
    tertiaryContainer = RainDarkTertiaryContainer,
    onTertiaryContainer = RainDarkOnTertiaryContainer,
    error = RainDarkError,
    onError = RainDarkOnError,
    errorContainer = RainDarkErrorContainer,
    onErrorContainer = RainDarkOnErrorContainer,
    background = RainDarkBackground,
    onBackground = RainDarkOnBackground,
    surface = RainDarkSurface,
    onSurface = RainDarkOnSurface,
    surfaceVariant = RainDarkSurfaceVariant,
    onSurfaceVariant = RainDarkOnSurfaceVariant,
    outline = RainDarkOutline,
    outlineVariant = RainDarkOutlineVariant
)

// Maple Color Schemes
private val MapleLightScheme = lightColorScheme(
    primary = MaplePrimary,
    onPrimary = MapleOnPrimary,
    primaryContainer = MaplePrimaryContainer,
    onPrimaryContainer = MapleOnPrimaryContainer,
    secondary = MapleSecondary,
    onSecondary = MapleOnSecondary,
    secondaryContainer = MapleSecondaryContainer,
    onSecondaryContainer = MapleOnSecondaryContainer,
    tertiary = MapleTertiary,
    onTertiary = MapleOnTertiary,
    tertiaryContainer = MapleTertiaryContainer,
    onTertiaryContainer = MapleOnTertiaryContainer,
    error = MapleError,
    onError = MapleOnError,
    errorContainer = MapleErrorContainer,
    onErrorContainer = MapleOnErrorContainer,
    background = MapleBackground,
    onBackground = MapleOnBackground,
    surface = MapleSurface,
    onSurface = MapleOnSurface,
    surfaceVariant = MapleSurfaceVariant,
    onSurfaceVariant = MapleOnSurfaceVariant,
    outline = MapleOutline,
    outlineVariant = MapleOutlineVariant
)

private val MapleDarkScheme = darkColorScheme(
    primary = MapleDarkPrimary,
    onPrimary = MapleDarkOnPrimary,
    primaryContainer = MapleDarkPrimaryContainer,
    onPrimaryContainer = MapleDarkOnPrimaryContainer,
    secondary = MapleDarkSecondary,
    onSecondary = MapleDarkOnSecondary,
    secondaryContainer = MapleDarkSecondaryContainer,
    onSecondaryContainer = MapleDarkOnSecondaryContainer,
    tertiary = MapleDarkTertiary,
    onTertiary = MapleDarkOnTertiary,
    tertiaryContainer = MapleDarkTertiaryContainer,
    onTertiaryContainer = MapleDarkOnTertiaryContainer,
    error = MapleDarkError,
    onError = MapleDarkOnError,
    errorContainer = MapleDarkErrorContainer,
    onErrorContainer = MapleDarkOnErrorContainer,
    background = MapleDarkBackground,
    onBackground = MapleDarkOnBackground,
    surface = MapleDarkSurface,
    onSurface = MapleDarkOnSurface,
    surfaceVariant = MapleDarkSurfaceVariant,
    onSurfaceVariant = MapleDarkOnSurfaceVariant,
    outline = MapleDarkOutline,
    outlineVariant = MapleDarkOutlineVariant
)

// Ocean Color Schemes
private val OceanLightScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer,
    tertiary = OceanTertiary,
    onTertiary = OceanOnTertiary,
    tertiaryContainer = OceanTertiaryContainer,
    onTertiaryContainer = OceanOnTertiaryContainer,
    error = OceanError,
    onError = OceanOnError,
    errorContainer = OceanErrorContainer,
    onErrorContainer = OceanOnErrorContainer,
    background = OceanBackground,
    onBackground = OceanOnBackground,
    surface = OceanSurface,
    onSurface = OceanOnSurface,
    surfaceVariant = OceanSurfaceVariant,
    onSurfaceVariant = OceanOnSurfaceVariant,
    outline = OceanOutline,
    outlineVariant = OceanOutlineVariant
)

private val OceanDarkScheme = darkColorScheme(
    primary = OceanDarkPrimary,
    onPrimary = OceanDarkOnPrimary,
    primaryContainer = OceanDarkPrimaryContainer,
    onPrimaryContainer = OceanDarkOnPrimaryContainer,
    secondary = OceanDarkSecondary,
    onSecondary = OceanDarkOnSecondary,
    secondaryContainer = OceanDarkSecondaryContainer,
    onSecondaryContainer = OceanDarkOnSecondaryContainer,
    tertiary = OceanDarkTertiary,
    onTertiary = OceanDarkOnTertiary,
    tertiaryContainer = OceanDarkTertiaryContainer,
    onTertiaryContainer = OceanDarkOnTertiaryContainer,
    error = OceanDarkError,
    onError = OceanDarkOnError,
    errorContainer = OceanDarkErrorContainer,
    onErrorContainer = OceanDarkOnErrorContainer,
    background = OceanDarkBackground,
    onBackground = OceanDarkOnBackground,
    surface = OceanDarkSurface,
    onSurface = OceanDarkOnSurface,
    surfaceVariant = OceanDarkSurfaceVariant,
    onSurfaceVariant = OceanDarkOnSurfaceVariant,
    outline = OceanDarkOutline,
    outlineVariant = OceanDarkOutlineVariant
)

// Sunset Color Schemes
private val SunsetLightScheme = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondaryContainer = SunsetOnSecondaryContainer,
    tertiary = SunsetTertiary,
    onTertiary = SunsetOnTertiary,
    tertiaryContainer = SunsetTertiaryContainer,
    onTertiaryContainer = SunsetOnTertiaryContainer,
    error = SunsetError,
    onError = SunsetOnError,
    errorContainer = SunsetErrorContainer,
    onErrorContainer = SunsetOnErrorContainer,
    background = SunsetBackground,
    onBackground = SunsetOnBackground,
    surface = SunsetSurface,
    onSurface = SunsetOnSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = SunsetOnSurfaceVariant,
    outline = SunsetOutline,
    outlineVariant = SunsetOutlineVariant
)

private val SunsetDarkScheme = darkColorScheme(
    primary = SunsetDarkPrimary,
    onPrimary = SunsetDarkOnPrimary,
    primaryContainer = SunsetDarkPrimaryContainer,
    onPrimaryContainer = SunsetDarkOnPrimaryContainer,
    secondary = SunsetDarkSecondary,
    onSecondary = SunsetDarkOnSecondary,
    secondaryContainer = SunsetDarkSecondaryContainer,
    onSecondaryContainer = SunsetDarkOnSecondaryContainer,
    tertiary = SunsetDarkTertiary,
    onTertiary = SunsetDarkOnTertiary,
    tertiaryContainer = SunsetDarkTertiaryContainer,
    onTertiaryContainer = SunsetDarkOnTertiaryContainer,
    error = SunsetDarkError,
    onError = SunsetDarkOnError,
    errorContainer = SunsetDarkErrorContainer,
    onErrorContainer = SunsetDarkOnErrorContainer,
    background = SunsetDarkBackground,
    onBackground = SunsetDarkOnBackground,
    surface = SunsetDarkSurface,
    onSurface = SunsetDarkOnSurface,
    surfaceVariant = SunsetDarkSurfaceVariant,
    onSurfaceVariant = SunsetDarkOnSurfaceVariant,
    outline = SunsetDarkOutline,
    outlineVariant = SunsetDarkOutlineVariant
)

// Forest Color Schemes
private val ForestLightScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestOnSecondaryContainer,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    tertiaryContainer = ForestTertiaryContainer,
    onTertiaryContainer = ForestOnTertiaryContainer,
    error = ForestError,
    onError = ForestOnError,
    errorContainer = ForestErrorContainer,
    onErrorContainer = ForestOnErrorContainer,
    background = ForestBackground,
    onBackground = ForestOnBackground,
    surface = ForestSurface,
    onSurface = ForestOnSurface,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = ForestOnSurfaceVariant,
    outline = ForestOutline,
    outlineVariant = ForestOutlineVariant
)

private val ForestDarkScheme = darkColorScheme(
    primary = ForestDarkPrimary,
    onPrimary = ForestDarkOnPrimary,
    primaryContainer = ForestDarkPrimaryContainer,
    onPrimaryContainer = ForestDarkOnPrimaryContainer,
    secondary = ForestDarkSecondary,
    onSecondary = ForestDarkOnSecondary,
    secondaryContainer = ForestDarkSecondaryContainer,
    onSecondaryContainer = ForestDarkOnSecondaryContainer,
    tertiary = ForestDarkTertiary,
    onTertiary = ForestDarkOnTertiary,
    tertiaryContainer = ForestDarkTertiaryContainer,
    onTertiaryContainer = ForestDarkOnTertiaryContainer,
    error = ForestDarkError,
    onError = ForestDarkOnError,
    errorContainer = ForestDarkErrorContainer,
    onErrorContainer = ForestDarkOnErrorContainer,
    background = ForestDarkBackground,
    onBackground = ForestDarkOnBackground,
    surface = ForestDarkSurface,
    onSurface = ForestDarkOnSurface,
    surfaceVariant = ForestDarkSurfaceVariant,
    onSurfaceVariant = ForestDarkOnSurfaceVariant,
    outline = ForestDarkOutline,
    outlineVariant = ForestDarkOutlineVariant
)

// Lavender Color Schemes
private val LavenderLightScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderPrimaryContainer,
    onPrimaryContainer = LavenderOnPrimaryContainer,
    secondary = LavenderSecondary,
    onSecondary = LavenderOnSecondary,
    secondaryContainer = LavenderSecondaryContainer,
    onSecondaryContainer = LavenderOnSecondaryContainer,
    tertiary = LavenderTertiary,
    onTertiary = LavenderOnTertiary,
    tertiaryContainer = LavenderTertiaryContainer,
    onTertiaryContainer = LavenderOnTertiaryContainer,
    error = LavenderError,
    onError = LavenderOnError,
    errorContainer = LavenderErrorContainer,
    onErrorContainer = LavenderOnErrorContainer,
    background = LavenderBackground,
    onBackground = LavenderOnBackground,
    surface = LavenderSurface,
    onSurface = LavenderOnSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onSurfaceVariant = LavenderOnSurfaceVariant,
    outline = LavenderOutline,
    outlineVariant = LavenderOutlineVariant
)

private val LavenderDarkScheme = darkColorScheme(
    primary = LavenderDarkPrimary,
    onPrimary = LavenderDarkOnPrimary,
    primaryContainer = LavenderDarkPrimaryContainer,
    onPrimaryContainer = LavenderDarkOnPrimaryContainer,
    secondary = LavenderDarkSecondary,
    onSecondary = LavenderDarkOnSecondary,
    secondaryContainer = LavenderDarkSecondaryContainer,
    onSecondaryContainer = LavenderDarkOnSecondaryContainer,
    tertiary = LavenderDarkTertiary,
    onTertiary = LavenderDarkOnTertiary,
    tertiaryContainer = LavenderDarkTertiaryContainer,
    onTertiaryContainer = LavenderDarkOnTertiaryContainer,
    error = LavenderDarkError,
    onError = LavenderDarkOnError,
    errorContainer = LavenderDarkErrorContainer,
    onErrorContainer = LavenderDarkOnErrorContainer,
    background = LavenderDarkBackground,
    onBackground = LavenderDarkOnBackground,
    surface = LavenderDarkSurface,
    onSurface = LavenderDarkOnSurface,
    surfaceVariant = LavenderDarkSurfaceVariant,
    onSurfaceVariant = LavenderDarkOnSurfaceVariant,
    outline = LavenderDarkOutline,
    outlineVariant = LavenderDarkOutlineVariant
)

// Desert Color Schemes
private val DesertLightScheme = lightColorScheme(
    primary = DesertPrimary,
    onPrimary = DesertOnPrimary,
    primaryContainer = DesertPrimaryContainer,
    onPrimaryContainer = DesertOnPrimaryContainer,
    secondary = DesertSecondary,
    onSecondary = DesertOnSecondary,
    secondaryContainer = DesertSecondaryContainer,
    onSecondaryContainer = DesertOnSecondaryContainer,
    tertiary = DesertTertiary,
    onTertiary = DesertOnTertiary,
    tertiaryContainer = DesertTertiaryContainer,
    onTertiaryContainer = DesertOnTertiaryContainer,
    error = DesertError,
    onError = DesertOnError,
    errorContainer = DesertErrorContainer,
    onErrorContainer = DesertOnErrorContainer,
    background = DesertBackground,
    onBackground = DesertOnBackground,
    surface = DesertSurface,
    onSurface = DesertOnSurface,
    surfaceVariant = DesertSurfaceVariant,
    onSurfaceVariant = DesertOnSurfaceVariant,
    outline = DesertOutline,
    outlineVariant = DesertOutlineVariant
)

private val DesertDarkScheme = darkColorScheme(
    primary = DesertDarkPrimary,
    onPrimary = DesertDarkOnPrimary,
    primaryContainer = DesertDarkPrimaryContainer,
    onPrimaryContainer = DesertDarkOnPrimaryContainer,
    secondary = DesertDarkSecondary,
    onSecondary = DesertDarkOnSecondary,
    secondaryContainer = DesertDarkSecondaryContainer,
    onSecondaryContainer = DesertDarkOnSecondaryContainer,
    tertiary = DesertDarkTertiary,
    onTertiary = DesertDarkOnTertiary,
    tertiaryContainer = DesertDarkTertiaryContainer,
    onTertiaryContainer = DesertDarkOnTertiaryContainer,
    error = DesertDarkError,
    onError = DesertDarkOnError,
    errorContainer = DesertDarkErrorContainer,
    onErrorContainer = DesertDarkOnErrorContainer,
    background = DesertDarkBackground,
    onBackground = DesertDarkOnBackground,
    surface = DesertDarkSurface,
    onSurface = DesertDarkOnSurface,
    surfaceVariant = DesertDarkSurfaceVariant,
    onSurfaceVariant = DesertDarkOnSurfaceVariant,
    outline = DesertDarkOutline,
    outlineVariant = DesertDarkOutlineVariant
)

// Aurora Color Schemes
private val AuroraLightScheme = lightColorScheme(
    primary = AuroraPrimary,
    onPrimary = AuroraOnPrimary,
    primaryContainer = AuroraPrimaryContainer,
    onPrimaryContainer = AuroraOnPrimaryContainer,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    secondaryContainer = AuroraSecondaryContainer,
    onSecondaryContainer = AuroraOnSecondaryContainer,
    tertiary = AuroraTertiary,
    onTertiary = AuroraOnTertiary,
    tertiaryContainer = AuroraTertiaryContainer,
    onTertiaryContainer = AuroraOnTertiaryContainer,
    error = AuroraError,
    onError = AuroraOnError,
    errorContainer = AuroraErrorContainer,
    onErrorContainer = AuroraOnErrorContainer,
    background = AuroraBackground,
    onBackground = AuroraOnBackground,
    surface = AuroraSurface,
    onSurface = AuroraOnSurface,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = AuroraOnSurfaceVariant,
    outline = AuroraOutline,
    outlineVariant = AuroraOutlineVariant
)

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraDarkPrimary,
    onPrimary = AuroraDarkOnPrimary,
    primaryContainer = AuroraDarkPrimaryContainer,
    onPrimaryContainer = AuroraDarkOnPrimaryContainer,
    secondary = AuroraDarkSecondary,
    onSecondary = AuroraDarkOnSecondary,
    secondaryContainer = AuroraDarkSecondaryContainer,
    onSecondaryContainer = AuroraDarkOnSecondaryContainer,
    tertiary = AuroraDarkTertiary,
    onTertiary = AuroraDarkOnTertiary,
    tertiaryContainer = AuroraDarkTertiaryContainer,
    onTertiaryContainer = AuroraDarkOnTertiaryContainer,
    error = AuroraDarkError,
    onError = AuroraDarkOnError,
    errorContainer = AuroraDarkErrorContainer,
    onErrorContainer = AuroraDarkOnErrorContainer,
    background = AuroraDarkBackground,
    onBackground = AuroraDarkOnBackground,
    surface = AuroraDarkSurface,
    onSurface = AuroraDarkOnSurface,
    surfaceVariant = AuroraDarkSurfaceVariant,
    onSurfaceVariant = AuroraDarkOnSurfaceVariant,
    outline = AuroraDarkOutline,
    outlineVariant = AuroraDarkOutlineVariant
)


// ============================================
// 💜 科技紫 (Tech Purple) Color Schemes
// ============================================
private val TechPurpleLightScheme = lightColorScheme(
    primary = TechPurplePrimary,
    onPrimary = TechPurpleOnPrimary,
    primaryContainer = TechPurplePrimaryContainer,
    onPrimaryContainer = TechPurpleOnPrimaryContainer,
    secondary = TechPurpleSecondary,
    onSecondary = TechPurpleOnSecondary,
    secondaryContainer = TechPurpleSecondaryContainer,
    onSecondaryContainer = TechPurpleOnSecondaryContainer,
    tertiary = TechPurpleTertiary,
    onTertiary = TechPurpleOnTertiary,
    tertiaryContainer = TechPurpleTertiaryContainer,
    onTertiaryContainer = TechPurpleOnTertiaryContainer,
    error = TechPurpleError,
    onError = TechPurpleOnError,
    errorContainer = TechPurpleErrorContainer,
    onErrorContainer = TechPurpleOnErrorContainer,
    background = TechPurpleBackground,
    onBackground = TechPurpleOnBackground,
    surface = TechPurpleSurface,
    onSurface = TechPurpleOnSurface,
    surfaceVariant = TechPurpleSurfaceVariant,
    onSurfaceVariant = TechPurpleOnSurfaceVariant,
    outline = TechPurpleOutline,
    outlineVariant = TechPurpleOutlineVariant
)

private val TechPurpleDarkScheme = darkColorScheme(
    primary = TechPurplePrimaryDark,
    onPrimary = TechPurpleOnPrimaryDark,
    primaryContainer = TechPurplePrimaryContainerDark,
    onPrimaryContainer = TechPurpleOnPrimaryContainerDark,
    secondary = TechPurpleSecondaryDark,
    onSecondary = TechPurpleOnSecondaryDark,
    secondaryContainer = TechPurpleSecondaryContainerDark,
    onSecondaryContainer = TechPurpleOnSecondaryContainerDark,
    tertiary = TechPurpleTertiaryDark,
    onTertiary = TechPurpleOnTertiaryDark,
    tertiaryContainer = TechPurpleTertiaryContainerDark,
    onTertiaryContainer = TechPurpleOnTertiaryContainerDark,
    error = TechPurpleErrorDark,
    onError = TechPurpleOnErrorDark,
    errorContainer = TechPurpleErrorContainerDark,
    onErrorContainer = TechPurpleOnErrorContainerDark,
    background = TechPurpleBackgroundDark,
    onBackground = TechPurpleOnBackgroundDark,
    surface = TechPurpleSurfaceDark,
    onSurface = TechPurpleOnSurfaceDark,
    surfaceVariant = TechPurpleSurfaceVariantDark,
    onSurfaceVariant = TechPurpleOnSurfaceVariantDark,
    outline = TechPurpleOutlineDark,
    outlineVariant = TechPurpleOutlineVariantDark
)

// ============================================
// 🐍 黑曼巴 (Black Mamba) Color Schemes
// ============================================
private val BlackMambaLightScheme = lightColorScheme(
    primary = BlackMambaPrimary,
    onPrimary = BlackMambaOnPrimary,
    primaryContainer = BlackMambaPrimaryContainer,
    onPrimaryContainer = BlackMambaOnPrimaryContainer,
    secondary = BlackMambaSecondary,
    onSecondary = BlackMambaOnSecondary,
    secondaryContainer = BlackMambaSecondaryContainer,
    onSecondaryContainer = BlackMambaOnSecondaryContainer,
    tertiary = BlackMambaTertiary,
    onTertiary = BlackMambaOnTertiary,
    tertiaryContainer = BlackMambaTertiaryContainer,
    onTertiaryContainer = BlackMambaOnTertiaryContainer,
    error = BlackMambaError,
    onError = BlackMambaOnError,
    errorContainer = BlackMambaErrorContainer,
    onErrorContainer = BlackMambaOnErrorContainer,
    background = BlackMambaBackground,
    onBackground = BlackMambaOnBackground,
    surface = BlackMambaSurface,
    onSurface = BlackMambaOnSurface,
    surfaceVariant = BlackMambaSurfaceVariant,
    onSurfaceVariant = BlackMambaOnSurfaceVariant,
    outline = BlackMambaOutline,
    outlineVariant = BlackMambaOutlineVariant
)

private val BlackMambaDarkScheme = darkColorScheme(
    primary = BlackMambaPrimaryDark,
    onPrimary = BlackMambaOnPrimaryDark,
    primaryContainer = BlackMambaPrimaryContainerDark,
    onPrimaryContainer = BlackMambaOnPrimaryContainerDark,
    secondary = BlackMambaSecondaryDark,
    onSecondary = BlackMambaOnSecondaryDark,
    secondaryContainer = BlackMambaSecondaryContainerDark,
    onSecondaryContainer = BlackMambaOnSecondaryContainerDark,
    tertiary = BlackMambaTertiaryDark,
    onTertiary = BlackMambaOnTertiaryDark,
    tertiaryContainer = BlackMambaTertiaryContainerDark,
    onTertiaryContainer = BlackMambaOnTertiaryContainerDark,
    error = BlackMambaErrorDark,
    onError = BlackMambaOnErrorDark,
    errorContainer = BlackMambaErrorContainerDark,
    onErrorContainer = BlackMambaOnErrorContainerDark,
    background = BlackMambaBackgroundDark,
    onBackground = BlackMambaOnBackgroundDark,
    surface = BlackMambaSurfaceDark,
    onSurface = BlackMambaOnSurfaceDark,
    surfaceVariant = BlackMambaSurfaceVariantDark,
    onSurfaceVariant = BlackMambaOnSurfaceVariantDark,
    outline = BlackMambaOutlineDark,
    outlineVariant = BlackMambaOutlineVariantDark
)

// ============================================
// 🕴️ 小黑紫 (Grey Style) Color Schemes
// ============================================
private val GreyStyleLightScheme = lightColorScheme(
    primary = GreyStylePrimary,
    onPrimary = GreyStyleOnPrimary,
    primaryContainer = GreyStylePrimaryContainer,
    onPrimaryContainer = GreyStyleOnPrimaryContainer,
    secondary = GreyStyleSecondary,
    onSecondary = GreyStyleOnSecondary,
    secondaryContainer = GreyStyleSecondaryContainer,
    onSecondaryContainer = GreyStyleOnSecondaryContainer,
    tertiary = GreyStyleTertiary,
    onTertiary = GreyStyleOnTertiary,
    tertiaryContainer = GreyStyleTertiaryContainer,
    onTertiaryContainer = GreyStyleOnTertiaryContainer,
    error = GreyStyleError,
    onError = GreyStyleOnError,
    errorContainer = GreyStyleErrorContainer,
    onErrorContainer = GreyStyleOnErrorContainer,
    background = GreyStyleBackground,
    onBackground = GreyStyleOnBackground,
    surface = GreyStyleSurface,
    onSurface = GreyStyleOnSurface,
    surfaceVariant = GreyStyleSurfaceVariant,
    onSurfaceVariant = GreyStyleOnSurfaceVariant,
    outline = GreyStyleOutline,
    outlineVariant = GreyStyleOutlineVariant
)

private val GreyStyleDarkScheme = darkColorScheme(
    primary = GreyStylePrimaryDark,
    onPrimary = GreyStyleOnPrimaryDark,
    primaryContainer = GreyStylePrimaryContainerDark,
    onPrimaryContainer = GreyStyleOnPrimaryContainerDark,
    secondary = GreyStyleSecondaryDark,
    onSecondary = GreyStyleOnSecondaryDark,
    secondaryContainer = GreyStyleSecondaryContainerDark,
    onSecondaryContainer = GreyStyleOnSecondaryContainerDark,
    tertiary = GreyStyleTertiaryDark,
    onTertiary = GreyStyleOnTertiaryDark,
    tertiaryContainer = GreyStyleTertiaryContainerDark,
    onTertiaryContainer = GreyStyleOnTertiaryContainerDark,
    error = GreyStyleErrorDark,
    onError = GreyStyleOnErrorDark,
    errorContainer = GreyStyleErrorContainerDark,
    onErrorContainer = GreyStyleOnErrorContainerDark,
    background = GreyStyleBackgroundDark,
    onBackground = GreyStyleOnBackgroundDark,
    surface = GreyStyleSurfaceDark,
    onSurface = GreyStyleOnSurfaceDark,
    surfaceVariant = GreyStyleSurfaceVariantDark,
    onSurfaceVariant = GreyStyleOnSurfaceVariantDark,
    outline = GreyStyleOutlineDark,
    outlineVariant = GreyStyleOutlineVariantDark
)
