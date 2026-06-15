package takagi.ru.saison.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import takagi.ru.saison.data.local.datastore.SeasonalTheme
import takagi.ru.saison.ui.theme.AmberPrimary
import takagi.ru.saison.ui.theme.AmberSecondary
import takagi.ru.saison.ui.theme.AmberSurfaceVariant
import takagi.ru.saison.ui.theme.AmberTertiary
import takagi.ru.saison.ui.theme.AuroraPrimary
import takagi.ru.saison.ui.theme.AuroraSecondary
import takagi.ru.saison.ui.theme.AuroraSurfaceVariant
import takagi.ru.saison.ui.theme.AuroraTertiary
import takagi.ru.saison.ui.theme.DesertPrimary
import takagi.ru.saison.ui.theme.DesertSecondary
import takagi.ru.saison.ui.theme.DesertSurfaceVariant
import takagi.ru.saison.ui.theme.DesertTertiary
import takagi.ru.saison.ui.theme.ForestPrimary
import takagi.ru.saison.ui.theme.ForestSecondary
import takagi.ru.saison.ui.theme.ForestSurfaceVariant
import takagi.ru.saison.ui.theme.ForestTertiary
import takagi.ru.saison.ui.theme.LavenderPrimary
import takagi.ru.saison.ui.theme.LavenderSecondary
import takagi.ru.saison.ui.theme.LavenderSurfaceVariant
import takagi.ru.saison.ui.theme.LavenderTertiary
import takagi.ru.saison.ui.theme.MaplePrimary
import takagi.ru.saison.ui.theme.MapleSecondary
import takagi.ru.saison.ui.theme.MapleSurfaceVariant
import takagi.ru.saison.ui.theme.MapleTertiary
import takagi.ru.saison.ui.theme.MintPrimary
import takagi.ru.saison.ui.theme.MintSecondary
import takagi.ru.saison.ui.theme.MintSurfaceVariant
import takagi.ru.saison.ui.theme.MintTertiary
import takagi.ru.saison.ui.theme.OceanPrimary
import takagi.ru.saison.ui.theme.OceanSecondary
import takagi.ru.saison.ui.theme.OceanSurfaceVariant
import takagi.ru.saison.ui.theme.OceanTertiary
import takagi.ru.saison.ui.theme.Pink40
import takagi.ru.saison.ui.theme.Purple40
import takagi.ru.saison.ui.theme.PurpleGrey40
import takagi.ru.saison.ui.theme.RainPrimary
import takagi.ru.saison.ui.theme.RainSecondary
import takagi.ru.saison.ui.theme.RainSurfaceVariant
import takagi.ru.saison.ui.theme.RainTertiary
import takagi.ru.saison.ui.theme.SakuraPrimary
import takagi.ru.saison.ui.theme.SakuraSecondary
import takagi.ru.saison.ui.theme.SakuraSurfaceVariant
import takagi.ru.saison.ui.theme.SakuraTertiary
import takagi.ru.saison.ui.theme.SnowPrimary
import takagi.ru.saison.ui.theme.SnowSecondary
import takagi.ru.saison.ui.theme.SnowSurfaceVariant
import takagi.ru.saison.ui.theme.SnowTertiary
import takagi.ru.saison.ui.theme.SunsetPrimary
import takagi.ru.saison.ui.theme.SunsetSecondary
import takagi.ru.saison.ui.theme.SunsetSurfaceVariant
import takagi.ru.saison.ui.theme.SunsetTertiary
import takagi.ru.saison.util.SeasonHelper

internal data class ThemePreviewColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surface: Color
)

@Composable
internal fun getThemePreviewColors(theme: SeasonalTheme): ThemePreviewColors {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    return when (theme) {
        SeasonalTheme.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val dynamicScheme = if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
                ThemePreviewColors(
                    primary = dynamicScheme.primary,
                    secondary = dynamicScheme.secondary,
                    tertiary = dynamicScheme.tertiary,
                    surface = dynamicScheme.surfaceVariant
                )
            } else {
                ThemePreviewColors(
                    primary = Purple40,
                    secondary = PurpleGrey40,
                    tertiary = Pink40,
                    surface = Color(0xFFFFFBFE)
                )
            }
        }
        SeasonalTheme.AUTO_SEASONAL -> {
            val currentSeasonTheme = SeasonHelper.getCurrentSeasonTheme()
            getThemePreviewColors(currentSeasonTheme)
        }
        SeasonalTheme.SAKURA -> ThemePreviewColors(
            primary = SakuraPrimary,
            secondary = SakuraSecondary,
            tertiary = SakuraTertiary,
            surface = SakuraSurfaceVariant
        )
        SeasonalTheme.MINT -> ThemePreviewColors(
            primary = MintPrimary,
            secondary = MintSecondary,
            tertiary = MintTertiary,
            surface = MintSurfaceVariant
        )
        SeasonalTheme.AMBER -> ThemePreviewColors(
            primary = AmberPrimary,
            secondary = AmberSecondary,
            tertiary = AmberTertiary,
            surface = AmberSurfaceVariant
        )
        SeasonalTheme.SNOW -> ThemePreviewColors(
            primary = SnowPrimary,
            secondary = SnowSecondary,
            tertiary = SnowTertiary,
            surface = SnowSurfaceVariant
        )
        SeasonalTheme.RAIN -> ThemePreviewColors(
            primary = RainPrimary,
            secondary = RainSecondary,
            tertiary = RainTertiary,
            surface = RainSurfaceVariant
        )
        SeasonalTheme.MAPLE -> ThemePreviewColors(
            primary = MaplePrimary,
            secondary = MapleSecondary,
            tertiary = MapleTertiary,
            surface = MapleSurfaceVariant
        )
        SeasonalTheme.OCEAN -> ThemePreviewColors(
            primary = OceanPrimary,
            secondary = OceanSecondary,
            tertiary = OceanTertiary,
            surface = OceanSurfaceVariant
        )
        SeasonalTheme.SUNSET -> ThemePreviewColors(
            primary = SunsetPrimary,
            secondary = SunsetSecondary,
            tertiary = SunsetTertiary,
            surface = SunsetSurfaceVariant
        )
        SeasonalTheme.FOREST -> ThemePreviewColors(
            primary = ForestPrimary,
            secondary = ForestSecondary,
            tertiary = ForestTertiary,
            surface = ForestSurfaceVariant
        )
        SeasonalTheme.LAVENDER -> ThemePreviewColors(
            primary = LavenderPrimary,
            secondary = LavenderSecondary,
            tertiary = LavenderTertiary,
            surface = LavenderSurfaceVariant
        )
        SeasonalTheme.DESERT -> ThemePreviewColors(
            primary = DesertPrimary,
            secondary = DesertSecondary,
            tertiary = DesertTertiary,
            surface = DesertSurfaceVariant
        )
        SeasonalTheme.AURORA -> ThemePreviewColors(
            primary = AuroraPrimary,
            secondary = AuroraSecondary,
            tertiary = AuroraTertiary,
            surface = AuroraSurfaceVariant
        )
    }
}
