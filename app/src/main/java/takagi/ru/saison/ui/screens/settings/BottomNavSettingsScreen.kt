package takagi.ru.saison.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.data.local.datastore.BottomNavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val bottomNavVisibility by viewModel.bottomNavVisibility.collectAsState()
    val bottomNavOrder by viewModel.bottomNavOrder.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("底部导航栏") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // 提示文字
            Text(
                text = "自定义底部导航栏显示的项目和顺序。至少保留一个可见项。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
            
            val visibleCount = bottomNavVisibility.visibleCount()
            
            // 所有标签都可配置
            val configurableTabs = bottomNavOrder
            
            configurableTabs.forEachIndexed { index, tab ->
                val isVisible = bottomNavVisibility.isVisible(tab)
                val switchEnabled = !isVisible || visibleCount > 1
                
                BottomNavConfigRow(
                    icon = tab.toIcon(),
                    title = tab.toLabel(),
                    subtitle = if (isVisible) "已显示" else "已隐藏",
                    checked = isVisible,
                    switchEnabled = switchEnabled,
                    canMoveUp = index > 0,
                    canMoveDown = index < configurableTabs.lastIndex,
                    onCheckedChange = { checked ->
                        viewModel.updateBottomNavVisibility(tab, checked)
                    },
                    onMoveUp = {
                        if (index > 0) {
                            // 在完整列表中找到实际位置
                            val actualIndex = bottomNavOrder.indexOf(tab)
                            val newOrder = bottomNavOrder.toMutableList().apply {
                                add(actualIndex - 1, removeAt(actualIndex))
                            }
                            viewModel.updateBottomNavOrder(newOrder)
                        }
                    },
                    onMoveDown = {
                        if (index < configurableTabs.lastIndex) {
                            // 在完整列表中找到实际位置
                            val actualIndex = bottomNavOrder.indexOf(tab)
                            val newOrder = bottomNavOrder.toMutableList().apply {
                                add(actualIndex + 1, removeAt(actualIndex))
                            }
                            viewModel.updateBottomNavOrder(newOrder)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BottomNavConfigRow(
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 标题和副标题
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
            
            // 上移按钮
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (canMoveUp) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            
            // 下移按钮
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (canMoveDown) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            
            // 开关
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = switchEnabled
            )
        }
    }
}

// 扩展函数：将 BottomNavTab 转换为图标
private fun BottomNavTab.toIcon(): ImageVector = when (this) {
    BottomNavTab.COURSE -> Icons.Default.School
    BottomNavTab.CALENDAR -> Icons.Default.CalendarToday
    BottomNavTab.TASKS -> Icons.Default.CheckCircle
    BottomNavTab.POMODORO -> Icons.Default.Timer
    BottomNavTab.SUBSCRIPTION -> Icons.Default.Star
    BottomNavTab.SETTINGS -> Icons.Default.Settings
}

// 扩展函数：将 BottomNavTab 转换为标签
private fun BottomNavTab.toLabel(): String = when (this) {
    BottomNavTab.COURSE -> "课程"
    BottomNavTab.CALENDAR -> "日历"
    BottomNavTab.TASKS -> "任务"
    BottomNavTab.POMODORO -> "专注"
    BottomNavTab.SUBSCRIPTION -> "订阅"
    BottomNavTab.SETTINGS -> "设置"
}
