package takagi.ru.saison.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import takagi.ru.saison.data.local.datastore.BottomNavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomNavBottomSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavVisibility by viewModel.bottomNavVisibility.collectAsState()
    val bottomNavOrder by viewModel.bottomNavOrder.collectAsState()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val configuration = LocalConfiguration.current

    val isTablet = configuration.screenWidthDp >= 600

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
                contentDescription = "底部导航栏设置"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "底部导航栏",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "自定义底部导航栏显示的项目和顺序。至少保留一个可见项。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val visibleCount = bottomNavVisibility.visibleCount()

                itemsIndexed(
                    items = bottomNavOrder,
                    key = { _, tab -> tab.name }
                ) { index, tab ->
                    val isVisible = bottomNavVisibility.isVisible(tab)
                    val switchEnabled = !isVisible || visibleCount > 1

                    BottomNavConfigCard(
                        icon = tab.toIcon(),
                        title = tab.toLabel(),
                        subtitle = if (isVisible) "已显示" else "已隐藏",
                        checked = isVisible,
                        switchEnabled = switchEnabled,
                        canMoveUp = index > 0,
                        canMoveDown = index < bottomNavOrder.lastIndex,
                        onCheckedChange = { checked ->
                            viewModel.updateBottomNavVisibility(tab, checked)
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newOrder = bottomNavOrder.toMutableList().apply {
                                    add(index - 1, removeAt(index))
                                }
                                viewModel.updateBottomNavOrder(newOrder)
                            }
                        },
                        onMoveDown = {
                            if (index < bottomNavOrder.lastIndex) {
                                val newOrder = bottomNavOrder.toMutableList().apply {
                                    add(index + 1, removeAt(index))
                                }
                                viewModel.updateBottomNavOrder(newOrder)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavConfigCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    switchEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (canMoveUp) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (canMoveDown) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = switchEnabled
            )
        }
    }
}

private fun BottomNavTab.toIcon(): ImageVector = when (this) {
    BottomNavTab.COURSE -> Icons.Default.School
    BottomNavTab.CALENDAR -> Icons.Default.CalendarToday
    BottomNavTab.TASKS -> Icons.Default.CheckCircle
    BottomNavTab.POMODORO -> Icons.Default.Timer
    BottomNavTab.SUBSCRIPTION -> Icons.Default.Star
    BottomNavTab.SETTINGS -> Icons.Default.Settings
}

private fun BottomNavTab.toLabel(): String = when (this) {
    BottomNavTab.COURSE -> "课程"
    BottomNavTab.CALENDAR -> "日历"
    BottomNavTab.TASKS -> "任务"
    BottomNavTab.POMODORO -> "专注"
    BottomNavTab.SUBSCRIPTION -> "订阅"
    BottomNavTab.SETTINGS -> "设置"
}
