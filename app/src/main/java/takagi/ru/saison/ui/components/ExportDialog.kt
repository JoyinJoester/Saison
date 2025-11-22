package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.Semester

/**
 * 导出对话框
 * 允许用户选择导出当前学期或所有学期
 */
@Composable
fun ExportDialog(
    semesters: List<Semester>,
    currentSemesterId: Long?,
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit
) {
    var exportMode by remember { mutableStateOf(ExportMode.CURRENT_SEMESTER) }
    var selectedSemesters by remember { mutableStateOf(setOf<Long>()) }
    
    // 初始化选中当前学期
    LaunchedEffect(currentSemesterId) {
        if (currentSemesterId != null && exportMode == ExportMode.CURRENT_SEMESTER) {
            selectedSemesters = setOf(currentSemesterId)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.export_courses))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 导出模式选择
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    Text(
                        text = stringResource(R.string.export_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 当前学期选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = exportMode == ExportMode.CURRENT_SEMESTER,
                                onClick = {
                                    exportMode = ExportMode.CURRENT_SEMESTER
                                    if (currentSemesterId != null) {
                                        selectedSemesters = setOf(currentSemesterId)
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportMode == ExportMode.CURRENT_SEMESTER,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.export_current_semester))
                    }
                    
                    // 所有学期选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = exportMode == ExportMode.ALL_SEMESTERS,
                                onClick = {
                                    exportMode = ExportMode.ALL_SEMESTERS
                                    selectedSemesters = semesters.map { it.id }.toSet()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportMode == ExportMode.ALL_SEMESTERS,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.export_all_semesters))
                    }
                    
                    // 自定义选择选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = exportMode == ExportMode.CUSTOM,
                                onClick = {
                                    exportMode = ExportMode.CUSTOM
                                    selectedSemesters = emptySet()
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportMode == ExportMode.CUSTOM,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.export_custom_selection))
                    }
                }
                
                // 自定义选择时显示学期列表
                if (exportMode == ExportMode.CUSTOM) {
                    HorizontalDivider()
                    
                    Text(
                        text = stringResource(R.string.select_semesters),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(semesters) { semester ->
                            SemesterCheckboxItem(
                                semester = semester,
                                isSelected = semester.id in selectedSemesters,
                                onToggle = {
                                    selectedSemesters = if (semester.id in selectedSemesters) {
                                        selectedSemesters - semester.id
                                    } else {
                                        selectedSemesters + semester.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val semesterIds = when (exportMode) {
                        ExportMode.CURRENT_SEMESTER -> listOfNotNull(currentSemesterId)
                        ExportMode.ALL_SEMESTERS -> semesters.map { it.id }
                        ExportMode.CUSTOM -> selectedSemesters.toList()
                    }
                    onExport(ExportOptions(exportMode, semesterIds))
                },
                enabled = when (exportMode) {
                    ExportMode.CURRENT_SEMESTER -> currentSemesterId != null
                    ExportMode.ALL_SEMESTERS -> semesters.isNotEmpty()
                    ExportMode.CUSTOM -> selectedSemesters.isNotEmpty()
                }
            ) {
                Text(text = stringResource(R.string.export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SemesterCheckboxItem(
    semester: Semester,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onToggle,
                role = Role.Checkbox
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = semester.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${semester.startDate} - ${semester.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 导出模式
 */
enum class ExportMode {
    CURRENT_SEMESTER,  // 当前学期
    ALL_SEMESTERS,     // 所有学期
    CUSTOM             // 自定义选择
}

/**
 * 导出选项
 */
data class ExportOptions(
    val mode: ExportMode,
    val semesterIds: List<Long>
)
