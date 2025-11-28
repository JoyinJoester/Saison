package takagi.ru.saison.ui.components.local

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.backup.ImportPreview

/**
 * 导入预览对话框
 * 显示即将导入的数据统计信息
 * 
 * @param preview 导入预览信息
 * @param onConfirm 确认导入回调
 * @param onDismiss 取消回调
 */
@Composable
fun ImportPreviewDialog(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.local_import_preview_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 文件类型
                InfoRow(
                    label = stringResource(R.string.local_import_preview_file_type),
                    value = stringResource(
                        if (preview.isZipFile) R.string.local_import_preview_zip_file
                        else R.string.local_import_preview_json_file
                    )
                )
                
                Divider()
                
                // 总计统计
                Text(
                    text = stringResource(R.string.local_import_preview_summary),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                SummaryCard(
                    icon = Icons.Default.CheckCircle,
                    label = stringResource(R.string.local_import_preview_total_items),
                    value = preview.totalItems.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = stringResource(R.string.local_import_preview_new_items),
                        value = preview.newItems.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = stringResource(R.string.local_import_preview_duplicate_items),
                        value = preview.duplicateItems.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 数据类型详情
                if (preview.dataTypes.isNotEmpty()) {
                    Divider()
                    
                    Text(
                        text = stringResource(R.string.local_import_preview_data_types),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        preview.dataTypes.forEach { (dataType, count) ->
                            DataTypeRow(
                                dataType = dataType,
                                count = count
                            )
                        }
                    }
                }
                
                // 警告信息
                if (preview.duplicateItems > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.local_import_preview_duplicate_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.local_import_preview_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.local_import_preview_cancel))
            }
        }
    )
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 摘要卡片
 */
@Composable
private fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 数据类型行
 */
@Composable
private fun DataTypeRow(
    dataType: DataType,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(getDataTypeNameRes(dataType)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
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
        DataType.PREFERENCES -> R.string.local_export_data_type_semesters // 使用相同的字符串
    }
}
