package takagi.ru.saison.ui.components.local

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.backup.RestoreSummary

/**
 * 导入摘要对话框
 * 显示导入操作的详细结果
 * 
 * @param summary 导入摘要信息
 * @param onDismiss 关闭对话框的回调
 */
@Composable
fun ImportSummaryDialog(
    summary: RestoreSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.local_import_summary_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 总计
                SummaryItem(
                    label = stringResource(R.string.local_import_summary_total),
                    value = summary.totalImported.toString(),
                    isTotal = true
                )
                
                Divider()
                
                // 各数据类型详情
                if (summary.importedTasks > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_tasks),
                        value = summary.importedTasks.toString()
                    )
                }
                
                if (summary.importedCourses > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_courses),
                        value = summary.importedCourses.toString()
                    )
                }
                
                if (summary.importedEvents > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_events),
                        value = summary.importedEvents.toString()
                    )
                }
                
                if (summary.importedRoutines > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_routines),
                        value = summary.importedRoutines.toString()
                    )
                }
                
                if (summary.importedSubscriptions > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_subscriptions),
                        value = summary.importedSubscriptions.toString()
                    )
                }
                
                if (summary.importedPomodoroSessions > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_pomodoros),
                        value = summary.importedPomodoroSessions.toString()
                    )
                }
                
                if (summary.importedSemesters > 0) {
                    SummaryItem(
                        label = stringResource(R.string.local_export_data_type_semesters),
                        value = summary.importedSemesters.toString()
                    )
                }
                
                if (summary.skippedDuplicates > 0) {
                    Divider()
                    SummaryItem(
                        label = stringResource(R.string.local_import_summary_skipped),
                        value = summary.skippedDuplicates.toString(),
                        isWarning = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

/**
 * 摘要项
 */
@Composable
private fun SummaryItem(
    label: String,
    value: String,
    isTotal: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isWarning -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        
        Text(
            text = value,
            style = if (isTotal) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.Bold,
            color = when {
                isTotal -> MaterialTheme.colorScheme.primary
                isWarning -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}
