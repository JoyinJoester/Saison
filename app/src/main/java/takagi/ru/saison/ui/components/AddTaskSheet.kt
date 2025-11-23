package takagi.ru.saison.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.Priority
import takagi.ru.saison.util.NaturalLanguageParser
import takagi.ru.saison.util.ParsedTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    existingTask: takagi.ru.saison.domain.model.Task? = null,
    onDismiss: () -> Unit,
    onTaskAdd: (String, LocalDateTime?, Priority, List<String>, String, Boolean, Set<java.time.DayOfWeek>) -> Unit,
    parser: NaturalLanguageParser,
    modifier: Modifier = Modifier
) {
    val isEditMode = existingTask != null
    
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var selectedPriority by remember { mutableStateOf(existingTask?.priority ?: Priority.MEDIUM) }
    var selectedDate by remember { mutableStateOf<LocalDateTime?>(existingTask?.dueDate) }
    var parsedTask by remember { mutableStateOf<ParsedTask?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var selectedRepeatType by remember { mutableStateOf("不重复") }
    var reminderEnabled by remember { mutableStateOf(existingTask?.reminderTime != null) }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }
    var selectedWeekDays by remember { mutableStateOf(setOf<java.time.DayOfWeek>()) }
    
    // 实时解析标题输入（仅在非编辑模式下）
    LaunchedEffect(title) {
        if (!isEditMode && title.isNotBlank()) {
            parsedTask = parser.parse(title)
            parsedTask?.let { parsed ->
                if (parsed.dueDate != null && selectedDate == null) {
                    selectedDate = LocalDateTime.of(
                        parsed.dueDate,
                        parsed.time ?: java.time.LocalTime.of(23, 59)
                    )
                }
                if (selectedPriority == Priority.MEDIUM) {
                    selectedPriority = parsed.priority
                }
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // 标题
            Text(
                text = if (isEditMode) "编辑任务" else "新建任务",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )
            
            // 可滚动内容区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            
            // 任务标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入任务标题") },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 快捷日期按钮 - 第一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        selectedDate = LocalDateTime.now().withHour(23).withMinute(59)
                    },
                    label = { Text("今天") },
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
                        selectedDate = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59)
                    },
                    label = { Text("明天") },
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
                        selectedDate = LocalDateTime.now().plusWeeks(1).withHour(23).withMinute(59)
                    },
                    label = { Text("下周") },
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
                    label = { Text("自定义") },
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
                visible = selectedDate != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedDate?.let { date ->
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
                                IconButton(onClick = { selectedDate = null }) {
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
                PriorityButton(
                    priority = Priority.URGENT,
                    selected = selectedPriority == Priority.URGENT,
                    onClick = { selectedPriority = Priority.URGENT },
                    modifier = Modifier.weight(1f)
                )
                PriorityButton(
                    priority = Priority.HIGH,
                    selected = selectedPriority == Priority.HIGH,
                    onClick = { selectedPriority = Priority.HIGH },
                    modifier = Modifier.weight(1f)
                )
                PriorityButton(
                    priority = Priority.MEDIUM,
                    selected = selectedPriority == Priority.MEDIUM,
                    onClick = { selectedPriority = Priority.MEDIUM },
                    modifier = Modifier.weight(1f)
                )
                PriorityButton(
                    priority = Priority.LOW,
                    selected = selectedPriority == Priority.LOW,
                    onClick = { selectedPriority = Priority.LOW },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 更多选项按钮
            TextButton(
                onClick = { showMoreOptions = !showMoreOptions },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showMoreOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showMoreOptions) "收起" else "更多选项")
            }
            
            // 展开的更多选项
            AnimatedVisibility(
                visible = showMoreOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
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
                                    text = if (selectedDate != null) {
                                        if (selectedRepeatType != "不重复") {
                                            "将在每次重复时提醒"
                                        } else {
                                            "将在 ${selectedDate!!.format(DateTimeFormatter.ofPattern("MM月dd日"))} 提醒"
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
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("描述") },
                        placeholder = { Text("添加任务描述...") },
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
            } // 关闭可滚动Column
            
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
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val cleanTitle = if (isEditMode) title else (parsedTask?.title ?: title)
                            onTaskAdd(
                                cleanTitle,
                                selectedDate,
                                selectedPriority,
                                if (isEditMode) emptyList() else (parsedTask?.tags ?: emptyList()),
                                selectedRepeatType,
                                reminderEnabled,
                                selectedWeekDays
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank()
                ) {
                    Icon(
                        if (isEditMode) Icons.Filled.Save else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "保存" else "添加")
                }
            }
            } // 关闭底部按钮Column
        } // 关闭外层Column
    } // 关闭ModalBottomSheet
    
    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
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
                            
                            selectedDate = if (selectedDate != null) {
                                LocalDateTime.of(localDate, selectedDate!!.toLocalTime())
                            } else {
                                LocalDateTime.of(localDate, java.time.LocalTime.of(23, 59))
                            }
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
    
    // 时间选择器
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDate?.hour ?: 23,
            initialMinute = selectedDate?.minute ?: 59,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = if (selectedDate != null) {
                            selectedDate!!.withHour(timePickerState.hour)
                                .withMinute(timePickerState.minute)
                        } else {
                            LocalDateTime.now()
                                .withHour(timePickerState.hour)
                                .withMinute(timePickerState.minute)
                        }
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
