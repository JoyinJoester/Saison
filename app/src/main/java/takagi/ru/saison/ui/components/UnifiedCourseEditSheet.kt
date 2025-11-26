package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.CourseGroup

/**
 * 统一课程编辑弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCourseEditSheet(
    courseGroup: CourseGroup,
    onDismiss: () -> Unit,
    onSaveBasicInfo: (String, String?, Int) -> Unit,
    onDeleteCourse: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: (Course) -> Unit,
    onDeleteSchedule: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    var courseName by remember { mutableStateOf(courseGroup.courseName) }
    var instructor by remember { mutableStateOf(courseGroup.instructor ?: "") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<Course?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编辑课程",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
            
            HorizontalDivider()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // 基本信息编辑区
                Text(
                    text = "基本信息",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("课程名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("教师姓名（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 颜色指示器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "课程颜色",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(courseGroup.color),
                        modifier = Modifier.size(32.dp)
                    ) {}
                }
                
                // 保存基本信息按钮
                Button(
                    onClick = {
                        onSaveBasicInfo(
                            courseName,
                            instructor.ifBlank { null },
                            courseGroup.color
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = courseName.isNotBlank()
                ) {
                    Text("保存基本信息")
                }
                
                HorizontalDivider()
                
                // 上课时间列表
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "上课时间",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${courseGroup.scheduleCount}次",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 上课时间项列表
                courseGroup.courses.forEach { course ->
                    CourseScheduleItemCard(
                        course = course,
                        onEdit = { onEditSchedule(course) },
                        onDelete = { scheduleToDelete = course }
                    )
                }
                
                // 添加上课时间按钮
                OutlinedButton(
                    onClick = onAddSchedule,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加上课时间")
                }
                
                HorizontalDivider()
                
                // 删除整个课程按钮
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除整个课程")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    // 删除整个课程确认对话框
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("将删除\"${courseGroup.courseName}\"的所有${courseGroup.scheduleCount}个上课时间，此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteCourse()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除单个上课时间确认对话框
    scheduleToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("确认删除") },
            text = {
                val message = if (courseGroup.scheduleCount == 1) {
                    "这是最后一个上课时间，删除后将删除整个课程。"
                } else {
                    "确定要删除这个上课时间吗？"
                }
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSchedule(course)
                        scheduleToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
