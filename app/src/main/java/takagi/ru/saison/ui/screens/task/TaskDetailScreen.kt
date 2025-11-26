package takagi.ru.saison.ui.screens.task

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.Priority
import takagi.ru.saison.ui.components.SubtaskList
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TaskDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task by viewModel.task.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }
    var selectedRepeatType by remember { mutableStateOf("不重复") }
    var selectedWeekDays by remember { mutableStateOf(setOf<java.time.DayOfWeek>()) }
    var reminderEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }
    
    task?.let { currentTask ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.task_edit_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                )
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                
                // 可滚动内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 任务标题
                    OutlinedTextField(
                        value = currentTask.title,
                        onValueChange = { 
                            viewModel.updateTask(currentTask.copy(title = it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("任务标题") },
                        singleLine = false,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 快捷日期按钮
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.updateTask(
                                    currentTask.copy(
                                        dueDate = LocalDateTime.now().withHour(23).withMinute(59)
                                    )
                                )
                            },
                            label = { 
                                Text(
                                    "今天",
                                    maxLines = 1
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.updateTask(
                                    currentTask.copy(
                                        dueDate = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59)
                                    )
                                )
                            },
                            label = { 
                                Text(
                                    "明天",
                                    maxLines = 1
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.updateTask(
                                    currentTask.copy(
                                        dueDate = LocalDateTime.now().plusWeeks(1).withHour(23).withMinute(59)
                                    )
                                )
                            },
                            label = { 
                                Text(
                                    "下周",
                                    maxLines = 1
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        FilterChip(
                            selected = false,
                            onClick = { showDatePicker = true },
                            label = { 
                                Text(
                                    "自定义",
                                    maxLines = 1
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    
                    // 已选日期显示
                    AnimatedVisibility(
                        visible = currentTask.dueDate != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        currentTask.dueDate?.let { date ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Column {
                                            Text(
                                                text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = date.format(DateTimeFormatter.ofPattern("HH:mm")),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { showTimePicker = true }) {
                                            Icon(
                                                Icons.Outlined.AccessTime,
                                                contentDescription = "修改时间",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        IconButton(onClick = { 
                                            viewModel.updateTask(currentTask.copy(dueDate = null))
                                        }) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "清除",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 优先级选择
                    Text(
                        text = "优先级",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.values().forEach { priority ->
                            PriorityButton(
                                priority = priority,
                                selected = currentTask.priority == priority,
                                onClick = { 
                                    viewModel.updateTask(currentTask.copy(priority = priority))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 直接显示所有选项
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                            // 重复设置
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Repeat,
                                        contentDescription = null,
                                        tint = if (selectedRepeatType != "不重复") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "重复",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                // 重复选项 - 使用两行布局
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 第一行：不重复、每天、每周
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = selectedRepeatType == "不重复",
                                            onClick = { selectedRepeatType = "不重复" },
                                            label = { Text("不重复") },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                        FilterChip(
                                            selected = selectedRepeatType == "每天",
                                            onClick = { selectedRepeatType = "每天" },
                                            label = { Text("每天") },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                        FilterChip(
                                            selected = selectedRepeatType == "每周",
                                            onClick = { selectedRepeatType = "每周" },
                                            label = { Text("每周") },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    // 第二行：每月、每年、自定义
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = selectedRepeatType == "每月",
                                            onClick = { selectedRepeatType = "每月" },
                                            label = { Text("每月") },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                        FilterChip(
                                            selected = selectedRepeatType == "每年",
                                            onClick = { selectedRepeatType = "每年" },
                                            label = { Text("每年") },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                        FilterChip(
                                            selected = selectedRepeatType == "自定义",
                                            onClick = { 
                                                showCustomRepeatDialog = true
                                            },
                                            label = { Text("自定义") },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    // 显示已选择的自定义天数
                                    if (selectedRepeatType == "自定义" && selectedWeekDays.isNotEmpty()) {
                                        Text(
                                            text = "每周 ${selectedWeekDays.sortedBy { it.value }.joinToString("、") { 
                                                when(it) {
                                                    java.time.DayOfWeek.MONDAY -> "周一"
                                                    java.time.DayOfWeek.TUESDAY -> "周二"
                                                    java.time.DayOfWeek.WEDNESDAY -> "周三"
                                                    java.time.DayOfWeek.THURSDAY -> "周四"
                                                    java.time.DayOfWeek.FRIDAY -> "周五"
                                                    java.time.DayOfWeek.SATURDAY -> "周六"
                                                    java.time.DayOfWeek.SUNDAY -> "周日"
                                                }
                                            }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                            
                            // 提醒设置
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (reminderEnabled) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else 
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                onClick = { reminderEnabled = !reminderEnabled }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Notifications,
                                                contentDescription = null,
                                                tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "提醒我",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Switch(
                                            checked = reminderEnabled,
                                            onCheckedChange = { reminderEnabled = it }
                                        )
                                    }
                                    if (reminderEnabled) {
                                        Text(
                                            text = if (currentTask.dueDate != null) {
                                                if (selectedRepeatType != "不重复") {
                                                    "将在每次重复时提醒"
                                                } else {
                                                    "将在 ${currentTask.dueDate!!.format(DateTimeFormatter.ofPattern("MM月dd日"))} 提醒"
                                                }
                                            } else {
                                                if (selectedRepeatType != "不重复") {
                                                    "将在每次重复时提醒"
                                                } else {
                                                    "将在任务当天提醒"
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // 描述输入
                            OutlinedTextField(
                                value = currentTask.description ?: "",
                                onValueChange = { 
                                    viewModel.updateTask(currentTask.copy(description = it))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("描述") },
                                placeholder = { Text("添加任务描述...") },
                                minLines = 3,
                                maxLines = 5
                            )
                            
                            // 子任务列表
                            if (currentTask.subtasks.isNotEmpty()) {
                                Text(
                                    text = "子任务",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                SubtaskList(
                                    subtasks = currentTask.subtasks,
                                    onSubtaskToggle = { subtaskId, isCompleted ->
                                        val updatedSubtasks = currentTask.subtasks.map {
                                            if (it.id == subtaskId) it.copy(isCompleted = isCompleted)
                                            else it
                                        }
                                        viewModel.updateTask(currentTask.copy(subtasks = updatedSubtasks))
                                    },
                                    onSubtaskAdd = { title ->
                                        // TODO: 实现添加子任务
                                    },
                                    onSubtaskDelete = { subtaskId ->
                                        val updatedSubtasks = currentTask.subtasks.filter { it.id != subtaskId }
                                        viewModel.updateTask(currentTask.copy(subtasks = updatedSubtasks))
                                    }
                                )
                            }
                        }
                    }
                
                // 底部按钮区域（固定在底部）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        
                        Button(
                            onClick = {
                                viewModel.updateTask(currentTask)
                                onNavigateBack()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除任务") },
            text = { Text("确定要删除这个任务吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 自定义重复对话框
    if (showCustomRepeatDialog) {
        CustomRepeatDialog(
            selectedDays = selectedWeekDays,
            onDismiss = { showCustomRepeatDialog = false },
            onConfirm = { days ->
                selectedWeekDays = days
                if (days.isNotEmpty()) {
                    selectedRepeatType = "自定义"
                } else {
                    selectedRepeatType = "不重复"
                }
                showCustomRepeatDialog = false
            }
        )
    }
    
    // 日期选择器
    if (showDatePicker) {
        task?.let { currentTask ->
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = currentTask.dueDate?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    ?: System.currentTimeMillis()
            )
            
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val localDate = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                                
                                val newDateTime = if (currentTask.dueDate != null) {
                                    LocalDateTime.of(localDate, currentTask.dueDate!!.toLocalTime())
                                } else {
                                    LocalDateTime.of(localDate, java.time.LocalTime.of(23, 59))
                                }
                                viewModel.updateTask(currentTask.copy(dueDate = newDateTime))
                            }
                            showDatePicker = false
                            showTimePicker = true
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
    
    // 时间选择器
    if (showTimePicker) {
        task?.let { currentTask ->
            val timePickerState = rememberTimePickerState(
                initialHour = currentTask.dueDate?.hour ?: 23,
                initialMinute = currentTask.dueDate?.minute ?: 59,
                is24Hour = true
            )
            
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newDateTime = if (currentTask.dueDate != null) {
                                currentTask.dueDate!!.withHour(timePickerState.hour)
                                    .withMinute(timePickerState.minute)
                            } else {
                                LocalDateTime.now()
                                    .withHour(timePickerState.hour)
                                    .withMinute(timePickerState.minute)
                            }
                            viewModel.updateTask(currentTask.copy(dueDate = newDateTime))
                            showTimePicker = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("取消")
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TimePicker(state = timePickerState)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRepeatDialog(
    selectedDays: Set<java.time.DayOfWeek>,
    onDismiss: () -> Unit,
    onConfirm: (Set<java.time.DayOfWeek>) -> Unit
) {
    var tempSelectedDays by remember { mutableStateOf(selectedDays) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自定义重复",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选择每周重复的日期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 星期选择器
                val weekDays = listOf(
                    java.time.DayOfWeek.MONDAY to "周一",
                    java.time.DayOfWeek.TUESDAY to "周二",
                    java.time.DayOfWeek.WEDNESDAY to "周三",
                    java.time.DayOfWeek.THURSDAY to "周四",
                    java.time.DayOfWeek.FRIDAY to "周五",
                    java.time.DayOfWeek.SATURDAY to "周六",
                    java.time.DayOfWeek.SUNDAY to "周日"
                )
                
                weekDays.forEach { (day, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = tempSelectedDays.contains(day),
                            onCheckedChange = { checked ->
                                tempSelectedDays = if (checked) {
                                    tempSelectedDays + day
                                } else {
                                    tempSelectedDays - day
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tempSelectedDays) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun PriorityButton(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (priority) {
        Priority.URGENT -> "紧急" to MaterialTheme.colorScheme.error
        Priority.HIGH -> "高" to MaterialTheme.colorScheme.tertiary
        Priority.MEDIUM -> "中" to MaterialTheme.colorScheme.primary
        Priority.LOW -> "低" to MaterialTheme.colorScheme.outline
    }
    
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Text(
                label,
                style = MaterialTheme.typography.labelLarge
            ) 
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.15f),
            selectedLabelColor = color
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) color else MaterialTheme.colorScheme.outline,
            selectedBorderColor = color,
            borderWidth = if (selected) 2.dp else 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
}
