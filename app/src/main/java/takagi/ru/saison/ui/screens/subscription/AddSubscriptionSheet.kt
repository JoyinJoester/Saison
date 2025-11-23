package takagi.ru.saison.ui.screens.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, Double, String, Int, LocalDate, String?, Boolean, Boolean, Int) -> Unit
) {
    val isEditMode = existingSubscription != null
    val defaultCategory = stringResource(R.string.subscription_default_category)
    
    var name by remember { mutableStateOf(existingSubscription?.name ?: "") }
    var category by remember { mutableStateOf(existingSubscription?.category ?: defaultCategory) }
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
    var note by remember { mutableStateOf(existingSubscription?.note ?: "") }
    var autoRenewal by remember { mutableStateOf(existingSubscription?.autoRenewal ?: true) }
    var reminderEnabled by remember { mutableStateOf(existingSubscription?.reminderEnabled ?: false) }
    var reminderDaysBefore by remember { mutableStateOf(existingSubscription?.reminderDaysBefore?.toString() ?: "1") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showCycleTypeDropdown by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.subscription_field_category)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) price = it },
                    label = { Text(stringResource(R.string.subscription_field_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                // Cycle Type Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    val cycleTypeDisplayText = when(cycleType) {
                        "MONTHLY" -> stringResource(R.string.subscription_cycle_monthly)
                        "QUARTERLY" -> stringResource(R.string.subscription_cycle_quarterly)
                        "YEARLY" -> stringResource(R.string.subscription_cycle_yearly)
                        else -> cycleType
                    }
                    OutlinedTextField(
                        value = cycleTypeDisplayText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.subscription_field_cycle_type)) },
                        trailingIcon = {
                            IconButton(onClick = { showCycleTypeDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showCycleTypeDropdown,
                        onDismissRequest = { showCycleTypeDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.subscription_cycle_monthly)) },
                            onClick = { cycleType = "MONTHLY"; showCycleTypeDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.subscription_cycle_quarterly)) },
                            onClick = { cycleType = "QUARTERLY"; showCycleTypeDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.subscription_cycle_yearly)) },
                            onClick = { cycleType = "YEARLY"; showCycleTypeDropdown = false }
                        )
                    }
                }
            }
            
            // Date Picker Trigger
            OutlinedTextField(
                value = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.subscription_field_start_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                enabled = false, // Disable text input, handle click on container or icon
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
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
                    val priceVal = price.toDoubleOrNull() ?: 0.0
                    val durationVal = cycleDuration.toIntOrNull() ?: 1
                    val reminderDaysVal = reminderDaysBefore.toIntOrNull() ?: 1
                    onSave(existingSubscription?.id, name, category, priceVal, cycleType, durationVal, startDate, note, autoRenewal, reminderEnabled, reminderDaysVal)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text(if (isEditMode) 
                    stringResource(R.string.subscription_save_button) 
                else 
                    stringResource(R.string.subscription_add_button))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.subscription_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.subscription_cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
