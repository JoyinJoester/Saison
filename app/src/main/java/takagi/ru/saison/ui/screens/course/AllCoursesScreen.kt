package takagi.ru.saison.ui.screens.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import takagi.ru.saison.ui.components.AddScheduleDialog
import takagi.ru.saison.ui.components.CourseGroupCard
import takagi.ru.saison.ui.components.UnifiedCourseEditSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCoursesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val courseGroups by viewModel.courseGroups.collectAsState()
    val selectedCourseGroup by viewModel.selectedCourseGroup.collectAsState()
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("所有课程") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (courseGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无课程",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = courseGroups,
                    key = { it.courseName }
                ) { courseGroup ->
                    CourseGroupCard(
                        courseGroup = courseGroup,
                        onClick = { viewModel.selectCourseGroup(courseGroup.courseName) }
                    )
                }
            }
        }
    }
    
    // 统一课程编辑弹窗
    selectedCourseGroup?.let { courseGroup ->
        UnifiedCourseEditSheet(
            courseGroup = courseGroup,
            onDismiss = { viewModel.clearSelectedCourseGroup() },
            onSaveBasicInfo = { name, instructor, color ->
                viewModel.updateCourseGroupInfo(
                    oldName = courseGroup.courseName,
                    newName = name,
                    instructor = instructor,
                    color = color
                )
                scope.launch {
                    snackbarHostState.showSnackbar("课程信息已更新")
                }
            },
            onDeleteCourse = {
                viewModel.deleteCourseGroup(courseGroup.courseName)
                viewModel.clearSelectedCourseGroup()
                scope.launch {
                    snackbarHostState.showSnackbar("课程已删除")
                }
            },
            onAddSchedule = {
                showAddScheduleDialog = true
            },
            onEditSchedule = { course ->
                // TODO: 实现编辑单个上课时间
            },
            onDeleteSchedule = { course ->
                viewModel.deleteCourse(course.id)
                // 刷新选中的课程组
                viewModel.selectCourseGroup(courseGroup.courseName)
                scope.launch {
                    snackbarHostState.showSnackbar("上课时间已删除")
                }
            }
        )
    }
    
    // 添加上课时间对话框
    if (showAddScheduleDialog && selectedCourseGroup != null) {
        AddScheduleDialog(
            existingCourses = selectedCourseGroup!!.courses,
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { day, periodStart, periodEnd, location, weekPattern, customWeeks ->
                viewModel.addScheduleToCourseGroup(
                    courseName = selectedCourseGroup!!.courseName,
                    dayOfWeek = day,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    location = location,
                    weekPattern = weekPattern,
                    customWeeks = customWeeks
                )
                showAddScheduleDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("上课时间已添加")
                }
            },
            onCheckConflict = { day, periodStart, periodEnd ->
                viewModel.checkPeriodConflict(day, periodStart, periodEnd)
            }
        )
    }
}
