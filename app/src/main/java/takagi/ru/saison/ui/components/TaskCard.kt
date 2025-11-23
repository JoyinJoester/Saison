package takagi.ru.saison.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.Priority
import takagi.ru.saison.domain.model.Task
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onTaskClick: () -> Unit,
    onToggleComplete: (Boolean) -> Unit,
    onToggleFavorite: ((Long) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isChecked by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val isOverdue = task.dueDate?.isBefore(LocalDateTime.now()) == true && !task.isCompleted
    
    // 确定卡片背景色 - 确保完全不透明
    val containerColor = when {
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    
    // 确定卡片透明度
    val alpha = if (task.isCompleted) 0.5f else 1f
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .then(
                if (onLongPress != null) {
                    @OptIn(ExperimentalFoundationApi::class)
                    Modifier.combinedClickable(
                        onClick = {
                            if (isMultiSelectMode && onSelectionToggle != null) {
                                onSelectionToggle()
                            } else {
                                onTaskClick()
                            }
                        },
                        onLongClick = onLongPress
                    )
                } else {
                    Modifier.clickable(onClick = onTaskClick)
                }
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 优先级指示条
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(getPriorityColor(task.priority))
                    .align(Alignment.CenterStart)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                    .then(
                        if (task.isCompleted) Modifier.alpha(alpha) else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 多选模式复选框
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle?.invoke() },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // 完成复选框
            if (!isMultiSelectMode) {
                IconButton(
                    onClick = {
                        isChecked = !isChecked
                        onToggleComplete(isChecked)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    AnimatedContent(
                        targetState = isChecked,
                        transitionSpec = {
                            scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                        },
                        label = "checkbox"
                    ) { checked ->
                        if (checked) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "已完成",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Circle,
                                contentDescription = "未完成",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // 任务内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 标题 - 单行显示
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                    color = if (isChecked) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 描述 - 最多2行
                task.description?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 元信息行
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 截止日期
                    task.dueDate?.let { dueDate ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOverdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = formatDueDate(dueDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    
                    // 重复方式
                    task.repeatRule?.let { rule ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = when (rule.frequency) {
                                    takagi.ru.saison.domain.model.Frequency.DAILY -> "每天"
                                    takagi.ru.saison.domain.model.Frequency.WEEKLY -> "每周"
                                    takagi.ru.saison.domain.model.Frequency.MONTHLY -> "每月"
                                    takagi.ru.saison.domain.model.Frequency.YEARLY -> "每年"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // 位置
                    task.location?.let { location ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // 子任务数量
                    if (task.subtasks.isNotEmpty()) {
                        val completedSubtasks = task.subtasks.count { it.isCompleted }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$completedSubtasks/${task.subtasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
            
            // 收藏星标按钮 - 右侧与完成按钮对齐
            if (onToggleFavorite != null) {
                IconButton(
                    onClick = { onToggleFavorite(task.id) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (task.isFavorite) "取消收藏" else "收藏",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.LOW -> Color(0xFF4CAF50)
        Priority.MEDIUM -> Color(0xFF2196F3)
        Priority.HIGH -> Color(0xFFFF9800)
        Priority.URGENT -> Color(0xFFF44336)
    }
}

private fun formatDueDate(dueDate: LocalDateTime): String {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val dueDay = dueDate.toLocalDate()
    
    return when {
        dueDay == today -> "今天 ${dueDate.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        dueDay == today.plusDays(1) -> "明天 ${dueDate.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        dueDay == today.minusDays(1) -> "昨天 ${dueDate.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        dueDay.year == today.year -> dueDate.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        else -> dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}
