package takagi.ru.saison.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.data.local.datastore.SeasonalTheme
import takagi.ru.saison.ui.components.PlusBadge

@Composable
internal fun ThemePreviewCard(
    theme: SeasonalTheme,
    themeName: String,
    isSelected: Boolean,
    isPremium: Boolean = false,
    isPlusActivated: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (isPremium && !isPlusActivated) 0.5f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = themeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isPremium) {
                        PlusBadge()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val themeColors = getThemePreviewColors(theme)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeColorSwatch(color = themeColors.primary)
                    ThemeColorSwatch(color = themeColors.secondary)
                    ThemeColorSwatch(color = themeColors.tertiary)
                    ThemeColorSwatch(color = themeColors.surface, showBorder = true)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ThemeColorSwatch(
    color: Color,
    showBorder: Boolean = false
) {
    val swatchModifier = Modifier
        .weight(1f)
        .height(32.dp)
        .background(
            color,
            shape = MaterialTheme.shapes.small
        )

    Box(
        modifier = if (showBorder) {
            swatchModifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            )
        } else {
            swatchModifier
        }
    )
}
