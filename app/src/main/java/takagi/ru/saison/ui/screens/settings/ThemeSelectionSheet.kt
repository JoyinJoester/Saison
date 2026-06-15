package takagi.ru.saison.ui.screens.settings

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import takagi.ru.saison.R
import takagi.ru.saison.data.local.datastore.SeasonalTheme
import takagi.ru.saison.domain.model.plus.PremiumThemes
import takagi.ru.saison.ui.components.PlusRequiredDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeBottomSheet(
    currentTheme: SeasonalTheme,
    isPlusActivated: Boolean,
    onThemeSelected: (SeasonalTheme) -> Unit,
    onNavigateToPlus: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current

    var showPlusDialog by remember { mutableStateOf(false) }
    var selectedPremiumTheme by remember { mutableStateOf<SeasonalTheme?>(null) }

    val isTablet = configuration.screenWidthDp >= 600

    LaunchedEffect(Unit) {
        announceForAccessibility(
            context = context,
            message = context.getString(R.string.settings_theme_dialog_title)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        ),
        modifier = modifier
            .then(
                if (isTablet) {
                    Modifier.widthIn(max = 600.dp)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .semantics {
                contentDescription = context.getString(R.string.settings_theme_dialog_title)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            ThemeList(
                currentTheme = currentTheme,
                isPlusActivated = isPlusActivated,
                onPremiumThemeBlocked = { theme ->
                    selectedPremiumTheme = theme
                    showPlusDialog = true
                },
                onThemeSelected = { theme, themeName ->
                    try {
                        onThemeSelected(theme)
                        announceForAccessibility(context, "已选择${themeName}主题")
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }
    }

    if (showPlusDialog && selectedPremiumTheme != null) {
        PlusRequiredDialog(
            themeName = getThemeName(selectedPremiumTheme!!),
            onNavigateToPlus = {
                showPlusDialog = false
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismiss()
                        onNavigateToPlus()
                    }
                }
            },
            onDismiss = {
                showPlusDialog = false
                selectedPremiumTheme = null
            }
        )
    }
}

@Composable
private fun ThemeList(
    currentTheme: SeasonalTheme,
    isPlusActivated: Boolean,
    onPremiumThemeBlocked: (SeasonalTheme) -> Unit,
    onThemeSelected: (SeasonalTheme, String) -> Unit
) {
    val sortedThemes = remember(isPlusActivated) {
        val allThemes = SeasonalTheme.values().toList()
        if (isPlusActivated) {
            allThemes
        } else {
            allThemes.sortedBy { theme ->
                PremiumThemes.isPremiumTheme(theme)
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(
            items = sortedThemes,
            key = { it.name }
        ) { theme ->
            val isSelected by remember {
                derivedStateOf { theme == currentTheme }
            }

            val themeName = getThemeName(theme)
            val isPremium = PremiumThemes.isPremiumTheme(theme)

            ThemePreviewCard(
                theme = theme,
                themeName = themeName,
                isSelected = isSelected,
                isPremium = isPremium,
                isPlusActivated = isPlusActivated,
                onClick = {
                    if (isPremium && !isPlusActivated) {
                        onPremiumThemeBlocked(theme)
                    } else {
                        onThemeSelected(theme, themeName)
                    }
                }
            )
        }
    }
}

@Composable
internal fun getThemeName(theme: SeasonalTheme): String {
    return when (theme) {
        SeasonalTheme.DYNAMIC -> stringResource(R.string.theme_dynamic)
        SeasonalTheme.AUTO_SEASONAL -> stringResource(R.string.theme_auto_seasonal)
        SeasonalTheme.SAKURA -> stringResource(R.string.theme_sakura)
        SeasonalTheme.MINT -> stringResource(R.string.theme_mint)
        SeasonalTheme.AMBER -> stringResource(R.string.theme_amber)
        SeasonalTheme.SNOW -> stringResource(R.string.theme_snow)
        SeasonalTheme.RAIN -> stringResource(R.string.theme_rain)
        SeasonalTheme.MAPLE -> stringResource(R.string.theme_maple)
        SeasonalTheme.OCEAN -> stringResource(R.string.theme_ocean)
        SeasonalTheme.SUNSET -> stringResource(R.string.theme_sunset)
        SeasonalTheme.FOREST -> stringResource(R.string.theme_forest)
        SeasonalTheme.LAVENDER -> stringResource(R.string.theme_lavender)
        SeasonalTheme.DESERT -> stringResource(R.string.theme_desert)
        SeasonalTheme.AURORA -> stringResource(R.string.theme_aurora)
    }
}

private fun announceForAccessibility(
    context: Context,
    message: String
) {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
        as? AccessibilityManager
    if (accessibilityManager?.isEnabled == true) {
        val event = AccessibilityEvent.obtain().apply {
            eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
            text.add(message)
        }
        accessibilityManager.sendAccessibilityEvent(event)
    }
}
