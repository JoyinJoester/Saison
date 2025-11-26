package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.Event
import takagi.ru.saison.domain.model.EventCategory
import takagi.ru.saison.util.EventDateFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventSheet(
    onDismiss: () -> Unit,
    onEventCreate: (Event) -> Unit,
    modifier: Modifier = Modifier,
    existingEvent: Event? = null
) {
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(existingEvent?.eventDate?.toLocalDate()) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(existingEvent?.eventDate?.toLocalTime()) }
    var selectedCategory by remember { mutableStateOf(existingEvent?.category ?: EventCategory.COUNTDOWN) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val isEditMode = existingEvent != null
    
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
                text = stringResource(
                    if (isEditMode) R.string.event_edit_title else R.string.event_create_title
                ),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 事件标题输入
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.event_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Title,
                            contentDescription = null
                        )
                    }
                )
                
                // 事件描述输入
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.event_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null
                        )
                    }
                )
                
                // 事件类别选择
                Text(
                    text = stringResource(R.string.event_category_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { 
                                Text(
                                    stringResource(category.getDisplayNameResId()),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                        )
                    }
                }
                
                // 日期选择
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedDate?.let { EventDateFormatter.formatDate(it) }
                                    ?: stringResource(R.string.event_select_date),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
                
                // 时间选择
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedTime?.let { EventDateFormatter.formatTime(it) }
                                    ?: stringResource(R.string.event_select_time),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
            
            // 底部按钮区域（固定在底部）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                
                Button(
                    onClick = {
                        if (title.isNotBlank() && selectedDate != null) {
                            val eventDateTime = LocalDateTime.of(
                                selectedDate,
                                selectedTime ?: LocalTime.of(0, 0)
                            )
                            val event = Event(
                                id = existingEvent?.id ?: 0,
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                eventDate = eventDateTime,
                                category = selectedCategory,
                                isCompleted = existingEvent?.isCompleted ?: false,
                                reminderEnabled = existingEvent?.reminderEnabled ?: false,
                                reminderTime = existingEvent?.reminderTime,
                                createdAt = existingEvent?.createdAt ?: LocalDateTime.now(),
                                updatedAt = LocalDateTime.now()
                            )
                            onEventCreate(event)
                        }
                    },
                    enabled = title.isNotBlank() && selectedDate != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Save else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(if (isEditMode) R.string.action_save else R.string.action_add))
                }
            }
        }
    }
    
    // 日期选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
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
                            selectedDate = localDate
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // 时间选择器对话框
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: 0,
            initialMinute = selectedTime?.minute ?: 0,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 清除按钮
                    TextButton(
                        onClick = {
                            selectedTime = null
                            showTimePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                    
                    // 确认按钮
                    TextButton(
                        onClick = {
                            selectedTime = LocalTime.of(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            showTimePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
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
