package takagi.ru.saison.ui.components.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.DataType

/**
 * 导出偏好设置卡片
 * 显示所有数据类型的切换开关和项目数量
 * 
 * @param preferences 当前的导出偏好设置
 * @param dataCounts 各数据类型的项目数量
 * @param onToggleDataType 切换数据类型的回调
 * @param onSelectAll 全选回调
 * @param onDeselectAll 取消全选回调
 * @param modifier Modifier
 */
@Composable
fun ExportPreferencesCard(
    preferences: BackupPreferences,
    dataCounts: Map<DataType, Int>,
    onToggleDataType: (DataType, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题和展开/收起按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.local_export_select_data_types),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开"
                )
            }
            
            // 展开时显示的内容
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 全选按钮
                    TextButton(
                        onClick = onSelectAll,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.local_export_select_all),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    // 取消全选按钮
                    TextButton(
                        onClick = onDeselectAll,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.local_export_deselect_all),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 数据类型列表
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DataTypeItem(
                        dataType = DataType.TASKS,
                        isEnabled = preferences.includeTasks,
                        count = dataCounts[DataType.TASKS] ?: 0,
                        onToggle = { onToggleDataType(DataType.TASKS, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.COURSES,
                        isEnabled = preferences.includeCourses,
                        count = dataCounts[DataType.COURSES] ?: 0,
                        onToggle = { onToggleDataType(DataType.COURSES, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.EVENTS,
                        isEnabled = preferences.includeEvents,
                        count = dataCounts[DataType.EVENTS] ?: 0,
                        onToggle = { onToggleDataType(DataType.EVENTS, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.ROUTINES,
                        isEnabled = preferences.includeRoutines,
                        count = dataCounts[DataType.ROUTINES] ?: 0,
                        onToggle = { onToggleDataType(DataType.ROUTINES, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.SUBSCRIPTIONS,
                        isEnabled = preferences.includeSubscriptions,
                        count = dataCounts[DataType.SUBSCRIPTIONS] ?: 0,
                        onToggle = { onToggleDataType(DataType.SUBSCRIPTIONS, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.POMODORO_SESSIONS,
                        isEnabled = preferences.includePomodoroSessions,
                        count = dataCounts[DataType.POMODORO_SESSIONS] ?: 0,
                        onToggle = { onToggleDataType(DataType.POMODORO_SESSIONS, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.PREFERENCES,
                        isEnabled = preferences.includePreferences,
                        count = dataCounts[DataType.PREFERENCES] ?: 0,
                        onToggle = { onToggleDataType(DataType.PREFERENCES, it) }
                    )
                    
                    DataTypeItem(
                        dataType = DataType.SEMESTERS,
                        isEnabled = preferences.includeSemesters,
                        count = dataCounts[DataType.SEMESTERS] ?: 0,
                        onToggle = { onToggleDataType(DataType.SEMESTERS, it) }
                    )
                }
            }
        }
    }
}

/**
 * 单个数据类型项
 */
@Composable
private fun DataTypeItem(
    dataType: DataType,
    isEnabled: Boolean,
    count: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(getDataTypeNameRes(dataType)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.local_export_item_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

/**
 * 获取数据类型的名称资源 ID
 */
private fun getDataTypeNameRes(dataType: DataType): Int {
    return when (dataType) {
        DataType.TASKS -> R.string.local_export_data_type_tasks
        DataType.COURSES -> R.string.local_export_data_type_courses
        DataType.EVENTS -> R.string.local_export_data_type_events
        DataType.ROUTINES -> R.string.local_export_data_type_routines
        DataType.SUBSCRIPTIONS -> R.string.local_export_data_type_subscriptions
        DataType.POMODORO_SESSIONS -> R.string.local_export_data_type_pomodoros
        DataType.SEMESTERS -> R.string.local_export_data_type_semesters
        DataType.PREFERENCES -> R.string.local_export_data_type_preferences
    }
}
