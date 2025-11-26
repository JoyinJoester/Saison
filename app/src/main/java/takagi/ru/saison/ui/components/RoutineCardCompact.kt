package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.routine.CycleType
import takagi.ru.saison.domain.model.routine.RoutineTaskWithStats

/**
 * 日程任务简化版卡片组件（用于日历页面）
 */
@Composable
fun RoutineCardCompact(
    taskWithStats: RoutineTaskWithStats,
    onCheckIn: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标、标题、周期类型
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 图标
                Icon(
                    imageVector = getIconForTask(taskWithStats.task.icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 标题
                    Text(
                        text = taskWithStats.task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 周期类型标识
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val cycleLabel = getCycleTypeLabel(taskWithStats.task.cycleType)
                        val cycleIcon = getCycleTypeIcon(taskWithStats.task.cycleType)
                        
                        Icon(
                            imageVector = cycleIcon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cycleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 中间：打卡次数
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "${taskWithStats.checkInCount}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (taskWithStats.checkInCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "次",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 右侧：打卡按钮
            FilledTonalIconButton(
                onClick = onCheckIn,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "打卡",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 获取周期类型标签
 */
private fun getCycleTypeLabel(cycleType: CycleType): String {
    return when (cycleType) {
        CycleType.DAILY -> "每日"
        CycleType.WEEKLY -> "每周"
        CycleType.MONTHLY -> "每月"
        CycleType.CUSTOM -> "自定义"
    }
}

/**
 * 获取周期类型图标
 */
private fun getCycleTypeIcon(cycleType: CycleType): ImageVector {
    return when (cycleType) {
        CycleType.DAILY -> Icons.Default.Today
        CycleType.WEEKLY -> Icons.Default.CalendarViewWeek
        CycleType.MONTHLY -> Icons.Default.CalendarMonth
        CycleType.CUSTOM -> Icons.Default.Tune
    }
}

/**
 * 根据图标名称获取对应的 Material Icon
 */
private fun getIconForTask(iconName: String?): ImageVector {
    return when (iconName) {
        "CheckCircle" -> Icons.Default.CheckCircle
        "Star" -> Icons.Default.Star
        "Favorite" -> Icons.Default.Favorite
        "Home" -> Icons.Default.Home
        "Work" -> Icons.Default.Work
        "School" -> Icons.Default.School
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Restaurant" -> Icons.Default.Restaurant
        "LocalCafe" -> Icons.Default.LocalCafe
        "Book" -> Icons.Default.Book
        "MusicNote" -> Icons.Default.MusicNote
        "Brush" -> Icons.Default.Brush
        else -> Icons.Default.Task
    }
}
