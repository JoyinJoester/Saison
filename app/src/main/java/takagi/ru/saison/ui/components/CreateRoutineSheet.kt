package takagi.ru.saison.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.routine.CycleConfig
import takagi.ru.saison.domain.model.routine.CycleType
import takagi.ru.saison.domain.model.routine.RoutineTask
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * 创建/编辑日程任务底部表单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSheet(
    task: RoutineTask? = null,
    onDismiss: () -> Unit,
    onSave: (RoutineTask) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(task?.icon ?: "CheckCircle") }
    var cycleType by remember { mutableStateOf(task?.cycleType ?: CycleType.DAILY) }
    var selectedDaysOfWeek by remember { 
        mutableStateOf(
            when (val config = task?.cycleConfig) {
                is CycleConfig.Weekly -> config.daysOfWeek.toSet()
                else -> emptySet()
            }
        )
    }
    var selectedDaysOfMonth by remember { 
        mutableStateOf(
            when (val config = task?.cycleConfig) {
                is CycleConfig.Monthly -> config.daysOfMonth.toSet()
                else -> emptySet()
            }
        )
    }
    var customIntervalDays by remember { 
        mutableStateOf(
            when (val config = task?.cycleConfig) {
                is CycleConfig.Custom -> {
                    // 从RRULE解析间隔天数，格式: "FREQ=DAILY;INTERVAL=X"
                    config.rrule.substringAfter("INTERVAL=", "2").toIntOrNull() ?: 2
                }
                else -> 2
            }
        )
    }
    
    var durationMinutes by remember { mutableStateOf<Int?>(task?.durationMinutes) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = if (task == null) "创建日程任务" else "编辑日程任务",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 任务标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { 
                    title = it
                    titleError = false
                },
                label = { Text("任务标题") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("请输入任务标题") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 任务描述输入
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("任务描述（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            // 图标选择
            OutlinedCard(
                onClick = { showIconPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("图标", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getIconByName(selectedIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }
            }
            
            // 时长选择
            OutlinedCard(
                onClick = { showDurationPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("活动时长", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = durationMinutes?.let { duration ->
                                if (duration > 0) {
                                    takagi.ru.saison.util.DurationFormatter.formatDuration(duration)
                                } else {
                                    "未设置"
                                }
                            } ?: "未设置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (durationMinutes != null && durationMinutes!! > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        // 清除按钮（仅当有时长时显示）
                        if (durationMinutes != null && durationMinutes!! > 0) {
                            IconButton(
                                onClick = { 
                                    durationMinutes = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除时长",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 周期类型选择
            Text(
                text = "周期类型",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CycleType.values().forEach { type ->
                    FilterChip(
                        selected = cycleType == type,
                        onClick = { cycleType = type },
                        label = { 
                            Text(
                                when (type) {
                                    CycleType.DAILY -> "每日"
                                    CycleType.WEEKLY -> "每周"
                                    CycleType.MONTHLY -> "每月"
                                    CycleType.CUSTOM -> "自定义"
                                }
                            )
                        }
                    )
                }
            }
            
            // 每周任务的星期选择器
            if (cycleType == CycleType.WEEKLY) {
                Text(
                    text = "选择星期",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysOfWeek = listOf(
                        DayOfWeek.MONDAY to "周一",
                        DayOfWeek.TUESDAY to "周二",
                        DayOfWeek.WEDNESDAY to "周三",
                        DayOfWeek.THURSDAY to "周四",
                        DayOfWeek.FRIDAY to "周五",
                        DayOfWeek.SATURDAY to "周六",
                        DayOfWeek.SUNDAY to "周日"
                    )
                    
                    daysOfWeek.chunked(4).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { (day, label) ->
                                FilterChip(
                                    selected = selectedDaysOfWeek.contains(day),
                                    onClick = {
                                        selectedDaysOfWeek = if (selectedDaysOfWeek.contains(day)) {
                                            selectedDaysOfWeek - day
                                        } else {
                                            selectedDaysOfWeek + day
                                        }
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 填充空白
                            repeat(4 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            // 每月任务的日期选择器
            if (cycleType == CycleType.MONTHLY) {
                Text(
                    text = "选择日期",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 显示 1-31 号
                    (1..31).chunked(7).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            chunk.forEach { day ->
                                FilterChip(
                                    selected = selectedDaysOfMonth.contains(day),
                                    onClick = {
                                        selectedDaysOfMonth = if (selectedDaysOfMonth.contains(day)) {
                                            selectedDaysOfMonth - day
                                        } else {
                                            selectedDaysOfMonth + day
                                        }
                                    },
                                    label = { Text("$day") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 填充空白
                            repeat(7 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            // 自定义周期配置
            if (cycleType == CycleType.CUSTOM) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "间隔天数",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "每 $customIntervalDays 天重复一次",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 减少按钮
                                FilledTonalIconButton(
                                    onClick = { 
                                        if (customIntervalDays > 2) {
                                            customIntervalDays--
                                        }
                                    },
                                    enabled = customIntervalDays > 2
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "减少"
                                    )
                                }
                                
                                // 滑块
                                Slider(
                                    value = customIntervalDays.toFloat(),
                                    onValueChange = { customIntervalDays = it.toInt() },
                                    valueRange = 2f..30f,
                                    steps = 27,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // 增加按钮
                                FilledTonalIconButton(
                                    onClick = { 
                                        if (customIntervalDays < 30) {
                                            customIntervalDays++
                                        }
                                    },
                                    enabled = customIntervalDays < 30
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "增加"
                                    )
                                }
                            }
                            
                            // 快捷选择按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 5, 7, 10, 14, 21, 30).forEach { days ->
                                    FilterChip(
                                        selected = customIntervalDays == days,
                                        onClick = { customIntervalDays = days },
                                        label = { Text("${days}天") }
                                    )
                                }
                            }
                            
                            // 说明文字
                            Text(
                                text = "例如：选择2天表示每隔一天重复，选择7天表示每周重复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 保存和取消按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        
                        // 验证周期配置
                        if (cycleType == CycleType.WEEKLY && selectedDaysOfWeek.isEmpty()) {
                            return@Button
                        }
                        if (cycleType == CycleType.MONTHLY && selectedDaysOfMonth.isEmpty()) {
                            return@Button
                        }
                        
                        val cycleConfig = when (cycleType) {
                            CycleType.DAILY -> CycleConfig.Daily()
                            CycleType.WEEKLY -> CycleConfig.Weekly(selectedDaysOfWeek.toList())
                            CycleType.MONTHLY -> CycleConfig.Monthly(selectedDaysOfMonth.sorted())
                            CycleType.CUSTOM -> CycleConfig.Custom("FREQ=DAILY;INTERVAL=$customIntervalDays")
                        }
                        
                        val now = LocalDateTime.now()
                        val newTask = RoutineTask(
                            id = task?.id ?: 0,
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            icon = selectedIcon,
                            cycleType = cycleType,
                            cycleConfig = cycleConfig,
                            durationMinutes = durationMinutes?.takeIf { it > 0 },
                            isActive = true,
                            createdAt = task?.createdAt ?: now,
                            updatedAt = now
                        )
                        
                        onSave(newTask)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (task == null) "创建" else "保存")
                }
            }
        }
    }
    
    // 图标选择器对话框
    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = selectedIcon,
            onIconSelected = { icon ->
                selectedIcon = icon
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
    
    // 时长选择器对话框
    if (showDurationPicker) {
        DurationPickerDialog(
            initialMinutes = durationMinutes?.takeIf { it > 0 },
            onDismiss = { showDurationPicker = false },
            onConfirm = { minutes ->
                durationMinutes = minutes
                showDurationPicker = false
            }
        )
    }
}

/**
 * 根据名称获取图标
 */
private fun getIconByName(name: String) = when (name) {
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
    else -> Icons.Default.CheckCircle
}
