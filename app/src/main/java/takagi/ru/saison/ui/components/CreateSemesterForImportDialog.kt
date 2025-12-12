package takagi.ru.saison.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSemesterForImportDialog(
    onDismiss: () -> Unit,
    onCreate: (semesterName: String, startDate: LocalDate, totalWeeks: Int) -> Unit
) {
    var semesterName by remember { mutableStateOf("") }
    var totalWeeks by remember { mutableStateOf("16") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.School, contentDescription = null)
        },
        title = {
            Text("创建新学期")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "导入的课程将添加到新学期中",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = semesterName,
                    onValueChange = { semesterName = it },
                    label = { Text("学期名称") },
                    placeholder = { Text("例如：2024-2025学年第一学期") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始日期: ${startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}")
                }
                
                OutlinedTextField(
                    value = totalWeeks,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            totalWeeks = it
                        }
                    },
                    label = { Text("总周数") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weeks = totalWeeks.toIntOrNull() ?: 16
                    onCreate(
                        semesterName.ifBlank { "新学期" },
                        startDate,
                        weeks.coerceIn(1, 52)
                    )
                },
                enabled = totalWeeks.toIntOrNull() != null
            ) {
                Text("创建并导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 日期选择器（简化版，实际可使用Material3的DatePicker）
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择开始日期") },
            text = {
                Column {
                    Text("当前选择: ${startDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { startDate = LocalDate.now() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("今天")
                        }
                        OutlinedButton(
                            onClick = { startDate = LocalDate.now().plusWeeks(1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下周")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("确定")
                }
            }
        )
    }
}
