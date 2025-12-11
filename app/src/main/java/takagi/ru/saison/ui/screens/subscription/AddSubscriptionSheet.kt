package takagi.ru.saison.ui.screens.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionSheet(
    existingSubscription: takagi.ru.saison.data.local.database.entities.SubscriptionEntity? = null,
    categories: List<takagi.ru.saison.data.local.database.entities.CategoryEntity> = emptyList(),
    lastSelectedCategory: String = "默认",
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, Double, String, Int, LocalDate, LocalDate?, String?, Boolean, Boolean, Int) -> Unit,
    onAddCategory: (String) -> Unit
) {
    val isEditMode = existingSubscription != null
    val defaultCategory = "默认"
    
    var name by remember { mutableStateOf(existingSubscription?.name ?: "") }
    // 如果是新建，使用上次选择的分类，如果上次选择的分类不存在（比如被删了），则使用默认
    var category by remember { 
        mutableStateOf(
            existingSubscription?.category ?: if (categories.any { it.name == lastSelectedCategory }) lastSelectedCategory else defaultCategory
        ) 
    }
    var price by remember { mutableStateOf(existingSubscription?.price?.toString() ?: "") }
    var cycleType by remember { mutableStateOf(existingSubscription?.cycleType ?: "MONTHLY") }
    var cycleDuration by remember { mutableStateOf(existingSubscription?.cycleDuration?.toString() ?: "1") }
    var startDate by remember { 
        mutableStateOf(
            existingSubscription?.let { 
                java.time.Instant.ofEpochMilli(it.startDate)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            } ?: LocalDate.now()
        ) 
    }
    var endDate by remember {
        mutableStateOf<LocalDate?>(
            existingSubscription?.endDate?.let {
                java.time.Instant.ofEpochMilli(it)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }
        )
    }
    var note by remember { mutableStateOf(existingSubscription?.note ?: "") }
    var autoRenewal by remember { mutableStateOf(existingSubscription?.autoRenewal ?: true) }
    var reminderEnabled by remember { mutableStateOf(existingSubscription?.reminderEnabled ?: false) }
    var reminderDaysBefore by remember { mutableStateOf(existingSubscription?.reminderDaysBefore?.toString() ?: "1") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var dateValidationError by remember { mutableStateOf<String?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    
    val endDateErrorMessage = stringResource(R.string.subscription_end_date_error)

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .imePadding(), // Avoid keyboard
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isEditMode) 
                    stringResource(R.string.subscription_edit_title) 
                else 
                    stringResource(R.string.subscription_add_title),
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.subscription_field_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Category Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.subscription_field_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    // 默认分类选项
                    DropdownMenuItem(
                        text = { Text("默认") },
                        onClick = { 
                            category = "默认"
                            categoryExpanded = false 
                        }
                    )
                    
                    // 显示所有非默认分类
                    categories.filter { !it.isDefault && it.name != "默认" }.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { 
                                category = cat.name
                                categoryExpanded = false 
                            }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 添加分类按钮
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("添加分类")
                            }
                        },
                        onClick = { 
                            showAddCategoryDialog = true
                            categoryExpanded = false 
                        }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) price = it },
                    label = { Text(stringResource(R.string.subscription_field_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                // Cycle Type Dropdown
                var cycleTypeExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    val cycleTypeDisplayText = when(cycleType) {
                        "MONTHLY" -> stringResource(R.string.subscription_cycle_monthly)
                        "QUARTERLY" -> stringResource(R.string.subscription_cycle_quarterly)
                        "YEARLY" -> stringResource(R.string.subscription_cycle_yearly)
                        else -> cycleType
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = cycleTypeExpanded,
                        onExpandedChange = { cycleTypeExpanded = !cycleTypeExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = cycleTypeDisplayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.subscription_field_cycle_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleTypeExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = cycleTypeExpanded,
                            onDismissRequest = { cycleTypeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.subscription_cycle_monthly)) },
                                onClick = { cycleType = "MONTHLY"; cycleTypeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.subscription_cycle_quarterly)) },
                                onClick = { cycleType = "QUARTERLY"; cycleTypeExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.subscription_cycle_yearly)) },
                                onClick = { cycleType = "YEARLY"; cycleTypeExpanded = false }
                            )
                        }
                    }
                }
            }
            
            // Start Date Picker
            OutlinedTextField(
                value = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.subscription_field_start_date)) },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            // End Date Picker (Optional)
            OutlinedTextField(
                value = endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: stringResource(R.string.subscription_field_end_date_optional),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.subscription_field_end_date)) },
                trailingIcon = {
                    Row {
                        if (endDate != null) {
                            IconButton(onClick = { endDate = null; dateValidationError = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showEndDatePicker = true },
                enabled = false,
                isError = dateValidationError != null,
                supportingText = dateValidationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (dateValidationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            // Auto-renewal Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.subscription_auto_renewal),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.subscription_auto_renewal_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoRenewal,
                    onCheckedChange = { autoRenewal = it }
                )
            }
            
            // Reminder Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.subscription_field_reminder),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }
            
            // Reminder Days Before (only visible when reminder is enabled)
            if (reminderEnabled) {
                OutlinedTextField(
                    value = reminderDaysBefore,
                    onValueChange = { if (it.all { char -> char.isDigit() }) reminderDaysBefore = it },
                    label = { Text(stringResource(R.string.subscription_field_reminder_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    // 验证日期
                    if (endDate != null && !endDate!!.isAfter(startDate)) {
                        dateValidationError = endDateErrorMessage
                        return@Button
                    }
                    
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val durationVal = cycleDuration.toIntOrNull() ?: 1
                    val reminderDaysVal = reminderDaysBefore.toIntOrNull() ?: 1
                    onSave(existingSubscription?.id, name, category, priceVal, cycleType, durationVal, startDate, endDate, note, autoRenewal, reminderEnabled, reminderDaysVal)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && price.isNotBlank() && dateValidationError == null
            ) {
                Text(if (isEditMode) 
                    stringResource(R.string.subscription_save_button) 
                else 
                    stringResource(R.string.subscription_add_button))
            }
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        // 重新验证结束日期
                        if (endDate != null && !endDate!!.isAfter(startDate)) {
                            dateValidationError = "结束日期必须晚于开始日期"
                        } else {
                            dateValidationError = null
                        }
                    }
                    showStartDatePicker = false
                }) {
                    Text(stringResource(R.string.subscription_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.subscription_cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // End Date Picker Dialog
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (endDate ?: startDate.plusMonths(1))
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        
                        if (selectedDate.isAfter(startDate)) {
                            endDate = selectedDate
                            dateValidationError = null
                        } else {
                            dateValidationError = "结束日期必须晚于开始日期"
                        }
                    }
                    showEndDatePicker = false
                }) {
                    Text(stringResource(R.string.subscription_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.subscription_cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("添加新分类") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName)
                            category = newCategoryName
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
