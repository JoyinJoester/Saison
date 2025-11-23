package takagi.ru.saison.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.domain.model.CourseSettings
import takagi.ru.saison.domain.model.CoursePeriod
import takagi.ru.saison.domain.model.ScheduleTemplate
import takagi.ru.saison.domain.model.ScheduleTemplates
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 课程设置底部面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSettingsSheet(
    currentSettings: CourseSettings,
    periods: List<CoursePeriod>,
    onDismiss: () -> Unit,
    onSave: (CourseSettings) -> Unit,
    onNavigateToSemesterManagement: () -> Unit = {},
    onNavigateToAllCourses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var settings by remember { mutableStateOf(currentSettings) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showWeekNumberDialog by remember { mutableStateOf(false) }
    var currentWeekInput by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "课程设置",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
            
            Divider()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 模板选择
                Text(
                    text = "选择模板",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                TemplateSelector(
                    templates = ScheduleTemplates.all,
                    selectedTemplateId = selectedTemplateId,
                    onTemplateSelected = { template ->
                        selectedTemplateId = template.id
                        settings = settings.copy(
                            totalPeriods = template.totalPeriods,
                            periodDuration = template.periodDuration,
                            breakDuration = template.breakDuration,
                            firstPeriodStartTime = template.firstPeriodStartTime,
                            lunchBreakDuration = template.lunchBreakDuration,
                            dinnerBreakDuration = template.dinnerBreakDuration
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 自定义配置
                Text(
                    text = "自定义配置",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 节次配置
                Text(
                    text = "节次配置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 每天总节次
                SettingSlider(
                    label = "每天节次",
                    value = settings.totalPeriods.toFloat(),
                    onValueChange = { 
                        settings = settings.copy(totalPeriods = it.roundToInt())
                        selectedTemplateId = null
                    },
                    valueRange = 4f..15f,
                    steps = 10,
                    valueText = "${settings.totalPeriods}节"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 时间配置
                Text(
                    text = "时间配置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 课程时长
                SettingSlider(
                    label = "课程时长",
                    value = settings.periodDuration.toFloat(),
                    onValueChange = { 
                        settings = settings.copy(periodDuration = (it.roundToInt() / 5) * 5)
                        selectedTemplateId = null
                    },
                    valueRange = 30f..120f,
                    steps = 17,
                    valueText = "${settings.periodDuration}分钟"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 课间休息
                SettingSlider(
                    label = "课间休息",
                    value = settings.breakDuration.toFloat(),
                    onValueChange = { 
                        settings = settings.copy(breakDuration = (it.roundToInt() / 5) * 5)
                        selectedTemplateId = null
                    },
                    valueRange = 5f..30f,
                    steps = 4,
                    valueText = "${settings.breakDuration}分钟"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 第一节课开始时间
                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第一节课开始时间",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = settings.firstPeriodStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 课程管理
                Text(
                    text = "课程管理",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 查看所有课程按钮
                OutlinedButton(
                    onClick = {
                        onDismiss() // 先关闭当前Sheet
                        onNavigateToAllCourses() // 然后导航到所有课程页面
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(takagi.ru.saison.R.string.course_action_view_all))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 学期管理
                Text(
                    text = "学期管理",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 这里将添加学期选择组件
                SemesterManagementSection(
                    modifier = Modifier.fillMaxWidth(),
                    onNavigateToSemesterManagement = {
                        onDismiss() // 先关闭当前Sheet
                        onNavigateToSemesterManagement() // 然后导航到学期管理
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 学期设置
                Text(
                    text = "学期设置",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 学期总周数
                SettingSlider(
                    label = "学期总周数",
                    value = settings.totalWeeks.toFloat(),
                    onValueChange = { 
                        settings = settings.copy(totalWeeks = it.roundToInt())
                    },
                    valueRange = 10f..25f,
                    steps = 14,
                    valueText = "${settings.totalWeeks}周"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 学期开始日期
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "学期第一周开始日期",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (settings.semesterStartDate != null) {
                                val currentWeek = calculateCurrentWeek(settings.semesterStartDate!!)
                                Text(
                                    text = "当前第 $currentWeek / ${settings.totalWeeks} 周",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = settings.semesterStartDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: "未设置",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 快速设置按钮
                OutlinedButton(
                    onClick = { showWeekNumberDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("输入当前周数自动设置")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 时间预览
                Text(
                    text = "时间预览",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                PeriodPreviewList(
                    periods = periods,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 保存按钮
            Button(
                onClick = { onSave(settings) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("保存设置")
            }
        }
    }
    
    // 时间选择器对话框
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = settings.firstPeriodStartTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                settings = settings.copy(firstPeriodStartTime = time)
                selectedTemplateId = null
                showTimePicker = false
            }
        )
    }
    
    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = settings.semesterStartDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                val updatedSettings = settings.copy(semesterStartDate = date)
                settings = updatedSettings
                // 立即保存设置
                onSave(updatedSettings)
                showDatePicker = false
                onDismiss()
            }
        )
    }
    
    // 周数输入对话框
    if (showWeekNumberDialog) {
        AlertDialog(
            onDismissRequest = { showWeekNumberDialog = false },
            title = { Text("设置当前周数") },
            text = {
                Column {
                    Text("输入当前是第几周,系统将自动计算学期开始日期")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = currentWeekInput,
                        onValueChange = { currentWeekInput = it.filter { char -> char.isDigit() } },
                        label = { Text("当前周数") },
                        placeholder = { Text("例如: 5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val weekNumber = currentWeekInput.toIntOrNull()
                        if (weekNumber != null && weekNumber > 0) {
                            val startDate = calculateSemesterStartFromWeek(weekNumber)
                            val updatedSettings = settings.copy(semesterStartDate = startDate)
                            settings = updatedSettings
                            // 立即保存设置
                            onSave(updatedSettings)
                            showWeekNumberDialog = false
                            currentWeekInput = ""
                            onDismiss()
                        }
                    },
                    enabled = currentWeekInput.toIntOrNull()?.let { it > 0 } == true
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showWeekNumberDialog = false
                    currentWeekInput = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 计算当前周数
 */
private fun calculateCurrentWeek(semesterStartDate: LocalDate): Int {
    val today = LocalDate.now()
    val daysSinceStart = ChronoUnit.DAYS.between(semesterStartDate, today)
    return ((daysSinceStart / 7) + 1).toInt().coerceAtLeast(1)
}

/**
 * 根据当前周数反推学期开始日期
 */
private fun calculateSemesterStartFromWeek(currentWeekNumber: Int): LocalDate {
    val today = LocalDate.now()
    val daysToSubtract = (currentWeekNumber - 1) * 7L
    val semesterStart = today.minusDays(daysToSubtract)
    
    // 调整到周一
    val dayOfWeek = semesterStart.dayOfWeek.value // 1=Monday, 7=Sunday
    val daysFromMonday = dayOfWeek - 1
    return semesterStart.minusDays(daysFromMonday.toLong())
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onConfirm(date)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@Composable
private fun SemesterManagementSection(
    modifier: Modifier = Modifier,
    onNavigateToSemesterManagement: () -> Unit = {}
) {
    val semesterViewModel: takagi.ru.saison.ui.screens.semester.SemesterViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val currentSemester by semesterViewModel.currentSemester.collectAsState()
    val allSemesters by semesterViewModel.allSemesters.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // 当前学期显示卡片
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前学期",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSemester?.name ?: "未选择学期",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentSemester != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentSemester!!.startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))} - ${currentSemester!!.endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 切换学期按钮
                    FilledTonalIconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            imageVector = if (expanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp 
                                         else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "收起" else "展开"
                        )
                    }
                }
                
                // 展开的学期列表
                androidx.compose.animation.AnimatedVisibility(
                    visible = expanded,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (allSemesters.isEmpty()) {
                            Text(
                                text = "暂无学期",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            allSemesters.forEach { semester ->
                                SemesterListItem(
                                    semester = semester,
                                    isSelected = semester.id == currentSemester?.id,
                                    onClick = {
                                        semesterViewModel.switchSemester(semester.id)
                                        expanded = false
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 管理学期按钮
        OutlinedButton(
            onClick = onNavigateToSemesterManagement,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("管理学期")
        }
    }
}

@Composable
private fun SemesterListItem(
    semester: takagi.ru.saison.domain.model.Semester,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = semester.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (semester.isArchived) {
                        AssistChip(
                            onClick = {},
                            label = { 
                                Text(
                                    text = "已归档",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${semester.startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))} - ${semester.endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
