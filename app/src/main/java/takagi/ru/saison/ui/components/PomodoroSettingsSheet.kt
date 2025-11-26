package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import takagi.ru.saison.ui.screens.pomodoro.PomodoroSettings
import takagi.ru.saison.domain.model.routine.RoutineTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsSheet(
    settings: PomodoroSettings,
    selectedTask: RoutineTask? = null,
    onSettingsChange: (PomodoroSettings) -> Unit,
    onTaskDurationChange: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempSettings by remember { mutableStateOf(settings) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 标题
            Text(
                text = if (selectedTask != null) {
                    stringResource(R.string.pomodoro_task_settings_title)
                } else {
                    stringResource(R.string.pomodoro_settings_dialog_title)
                },
                style = MaterialTheme.typography.titleLarge
            )
            
            // 任务信息展示(如果有选中任务)
            if (selectedTask != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = selectedTask.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        if (selectedTask.durationMinutes != null && onTaskDurationChange != null) {
                            var taskDuration by remember { mutableStateOf(selectedTask.durationMinutes.toString()) }
                            
                            OutlinedTextField(
                                value = taskDuration,
                                onValueChange = { taskDuration = it },
                                label = { Text(stringResource(R.string.pomodoro_task_duration_label)) },
                                suffix = { Text(stringResource(R.string.common_unit_minutes)) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
                
                Divider()
            }
            
            // 工作时长
            SettingSlider(
                label = stringResource(R.string.pomodoro_work_duration_label),
                value = tempSettings.workDuration,
                valueRange = 15f..60f,
                steps = 8,
                onValueChange = { tempSettings = tempSettings.copy(workDuration = it.toInt()) }
            )
            
            Divider()
            
            // 声音提醒
            SettingSwitch(
                label = stringResource(R.string.pomodoro_settings_sound),
                checked = tempSettings.soundEnabled,
                onCheckedChange = { tempSettings = tempSettings.copy(soundEnabled = it) }
            )
            
            // 震动提醒
            SettingSwitch(
                label = stringResource(R.string.pomodoro_settings_vibration),
                checked = tempSettings.vibrationEnabled,
                onCheckedChange = { tempSettings = tempSettings.copy(vibrationEnabled = it) }
            )
            
            // 保存按钮
            Button(
                onClick = {
                    onSettingsChange(tempSettings)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_action_confirm))
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                text = "$value 分钟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
