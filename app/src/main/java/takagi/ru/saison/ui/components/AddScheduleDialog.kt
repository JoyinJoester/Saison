package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.WeekPattern
import java.time.DayOfWeek

/**
 * 添加新上课时间的对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    existingCourses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (DayOfWeek, Int, Int, String?, WeekPattern, List<Int>?) -> Unit,
    onCheckConflict: (DayOfWeek, Int, Int) -> List<Course>,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var periodStart by remember { mutableStateOf<Int?>(null) }
    var periodEnd by remember { mutableStateOf<Int?>(null) }
    var location by remember { mutableStateOf("") }
    var weekPattern by remember { mutableStateOf(WeekPattern.ALL) }
    var conflictCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    
    // 检查冲突
    LaunchedEffect(selectedDay, periodStart, periodEnd) {
        if (selectedDay != null && periodStart != null && periodEnd != null) {
            conflictCourses = onCheckConflict(selectedDay!!, periodStart!!, periodEnd!!)
        } else {
            conflictCourses = emptyList()
        }
    }
    
    val canConfirm = selectedDay != null && 
                     periodStart != null && 
                     periodEnd != null && 
                     periodStart!! <= periodEnd!! &&
                     conflictCourses.isEmpty()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = "添加上课时间",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // 星期选择
                Text(
                    text = "星期",
                    style = MaterialTheme.typography.labelLarge
                )
                DayOfWeekSelector(
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = it }
                )
                
                // 节次选择
                Text(
                    text = "节次",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 开始节次
                    OutlinedTextField(
                        value = periodStart?.toString() ?: "",
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                if (value in 1..15) periodStart = value
                            }
                        },
                        label = { Text("开始节次") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    Text(
                        text = "至",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    // 结束节次
                    OutlinedTextField(
                        value = periodEnd?.toString() ?: "",
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                if (value in 1..15) periodEnd = value
                            }
                        },
                        label = { Text("结束节次") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // 教室
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("教室（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 周数模式
                Text(
                    text = "周数模式",
                    style = MaterialTheme.typography.labelLarge
                )
                WeekPatternSelector(
                    selectedPattern = weekPattern,
                    onPatternSelected = { weekPattern = it }
                )
                
                // 冲突警告
                if (conflictCourses.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "时间冲突",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            conflictCourses.forEach { course ->
                                Text(
                                    text = "• ${course.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // 按钮
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                selectedDay!!,
                                periodStart!!,
                                periodEnd!!,
                                location.ifBlank { null },
                                weekPattern,
                                null // customWeeks暂时不支持
                            )
                        },
                        enabled = canConfirm
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekSelector(
    selectedDay: DayOfWeek?,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        DayOfWeek.MONDAY to "周一",
        DayOfWeek.TUESDAY to "周二",
        DayOfWeek.WEDNESDAY to "周三",
        DayOfWeek.THURSDAY to "周四",
        DayOfWeek.FRIDAY to "周五",
        DayOfWeek.SATURDAY to "周六",
        DayOfWeek.SUNDAY to "周日"
    )
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { (day, name) ->
            FilterChip(
                selected = selectedDay == day,
                onClick = { onDaySelected(day) },
                label = { Text(name) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekPatternSelector(
    selectedPattern: WeekPattern,
    onPatternSelected: (WeekPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    val patterns = listOf(
        WeekPattern.ALL to "全周",
        WeekPattern.ODD to "单周",
        WeekPattern.EVEN to "双周"
    )
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        patterns.forEach { (pattern, name) ->
            FilterChip(
                selected = selectedPattern == pattern,
                onClick = { onPatternSelected(pattern) },
                label = { Text(name) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
