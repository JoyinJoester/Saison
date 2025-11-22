package takagi.ru.saison.ui.screens.course

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.CoursePeriod
import takagi.ru.saison.domain.model.WeekPattern
import takagi.ru.saison.ui.components.CourseCard
import takagi.ru.saison.ui.components.EditCourseSheet
// import takagi.ru.saison.ui.components.ExportCoursesDialog
// import takagi.ru.saison.ui.components.ExportSuccessDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    viewModel: CourseViewModel = hiltViewModel(),
    onCourseClick: (Long) -> Unit = {},
    onNavigateToImportPreview: (Uri, Long) -> Unit = { _, _ -> },
    onNavigateToAllCourses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coursesByDay by viewModel.coursesByDay.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val courseSettings by viewModel.courseSettings.collectAsState()
    val periods by viewModel.periods.collectAsState()
    val currentSemesterId by viewModel.currentSemesterIdState.collectAsState()
    val currentPeriod by viewModel.currentPeriod.collectAsState()
    val currentDay by viewModel.currentDay.collectAsState()
    val showWeekSelectorSheet by viewModel.showWeekSelectorSheet.collectAsState()
    
    // 更新当前节次
    LaunchedEffect(Unit) {
        viewModel.updateCurrentPeriod()
    }
    
    var showAddSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSemesterSettingsSheet by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }
    
    // 文件选择器 - 用于导入ICS文件
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(it)?.use { stream ->
                        stream.bufferedReader().use { reader ->
                            reader.readText()
                        }
                    }
                    if (content != null) {
                        takagi.ru.saison.util.TempFileCache.store(content)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val semesterId = currentSemesterId ?: 1L
                            onNavigateToImportPreview(it, semesterId)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CourseScreen", "Failed to read file", e)
                }
            }
        }
    }
    
    // 文件保存器 - 用于导出JSON文件（使用SAF）
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val exportOptions = viewModel.exportOptions.value
                if (exportOptions != null) {
                    viewModel.exportToUri(it, exportOptions.semesterIds)
                }
            }
        }
    }
    
    // Snackbar支持
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听导出状态
    val exportState by viewModel.exportState.collectAsState()
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "导出成功",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "导出失败: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            else -> {}
        }
    }
    
    val semesterViewModel: takagi.ru.saison.ui.screens.semester.SemesterViewModel = hiltViewModel()
    var showSemesterList by remember { mutableStateOf(false) }
    
    val totalWeeks = courseSettings.totalWeeks
    val baseWeek: Int = remember(courseSettings.semesterStartDate) {
        getCurrentWeekNumber(courseSettings.semesterStartDate)
    }
    
    val initialPage: Int = remember(baseWeek, totalWeeks) { 
        (baseWeek - 1).coerceIn(0, totalWeeks - 1)
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalWeeks }
    )
    
    LaunchedEffect(pagerState.currentPage, initialPage) {
        val offset = pagerState.currentPage - initialPage
        viewModel.setWeekOffset(offset)
    }
    
    LaunchedEffect(weekOffset) {
        val targetPage = initialPage + weekOffset
        if (targetPage != pagerState.currentPage && targetPage in 0 until totalWeeks) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加课程") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Custom Header
            CourseHeader(
                currentWeek = pagerState.currentPage + 1,
                totalWeeks = totalWeeks,
                onBackToCurrentWeek = { viewModel.goToCurrentWeek() },
                onSettingsClick = { showSettingsSheet = true },
                onSemesterSettingsClick = { showSemesterSettingsSheet = true },
                onImportClick = { importLauncher.launch(arrayOf("text/calendar", "text/x-vcalendar", "*/*")) },
                onExportClick = {
                    // 显示导出对话框
                    viewModel.showExportDialog()
                }
            )

            // Week Days Header
            val displayedWeekStart = remember(courseSettings.semesterStartDate, pagerState.currentPage) {
                val baseDate = courseSettings.semesterStartDate ?: LocalDate.now()
                val weekDate = baseDate.plusWeeks(pagerState.currentPage.toLong())
                // Ensure we start from Monday to match the GridTimetableView layout
                weekDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
            
            WeekHeader(
                startDate = displayedWeekStart,
                currentDay = if (pagerState.currentPage == initialPage) currentDay else null
            )

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageWeek = page + 1
                val filteredCoursesByDay = remember(coursesByDay, pageWeek) {
                    coursesByDay.mapValues { (_, courses) ->
                        courses.filter { viewModel.isCourseActiveInWeek(it, pageWeek) }
                    }
                }

                TimetableGrid(
                    coursesByDay = filteredCoursesByDay,
                    periods = periods,
                    onCourseClick = { courseId ->
                        val course = filteredCoursesByDay.values.flatten().find { it.id == courseId }
                        courseToEdit = course
                    },
                    onEmptyCellClick = { day, period -> showAddSheet = true }
                )
            }
        }
        
        // Error Snackbar
        when (val state = uiState) {
            is CourseUiState.Error -> {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(state.message) }
            }
            else -> {}
        }
    }
    
    // Sheets and Dialogs
    if (showSettingsSheet) {
        takagi.ru.saison.ui.components.CourseSettingsSheet(
            currentSettings = courseSettings,
            periods = periods,
            onDismiss = { showSettingsSheet = false },
            onSave = { newSettings ->
                viewModel.updateSettings(newSettings)
                showSettingsSheet = false
            },
            onNavigateToSemesterManagement = { showSemesterList = true }
        )
    }
    
    if (showSemesterSettingsSheet) {
        takagi.ru.saison.ui.components.SemesterSettingsSheet(
            currentSettings = courseSettings,
            onDismiss = { showSemesterSettingsSheet = false },
            onSave = { newSettings ->
                viewModel.updateSettings(newSettings)
                showSemesterSettingsSheet = false
            }
        )
    }
    
    if (showSemesterList) {
        takagi.ru.saison.ui.screens.semester.SemesterListScreen(
            viewModel = semesterViewModel,
            onNavigateBack = { showSemesterList = false }
        )
    }
    
    if (showAddSheet) {
        val allCourses = coursesByDay.values.flatten()
        takagi.ru.saison.ui.components.AddCourseSheet(
            periods = periods,
            occupiedPeriods = emptySet(),
            existingCourses = allCourses,
            onDismiss = { showAddSheet = false },
            onSave = { course ->
                viewModel.addCourse(course)
                showAddSheet = false
            }
        )
    }
    
    courseToEdit?.let { course ->
        val allCourses = coursesByDay.values.flatten().filter { it.id != course.id }
        EditCourseSheet(
            course = course,
            periods = periods,
            occupiedPeriods = emptySet(),
            existingCourses = allCourses,
            onDismiss = { courseToEdit = null },
            onSave = { updatedCourse ->
                viewModel.updateCourse(updatedCourse)
                courseToEdit = null
            },
            onDelete = {
                viewModel.deleteCourse(course.id)
                courseToEdit = null
            }
        )
    }
    
    if (showWeekSelectorSheet) {
        takagi.ru.saison.ui.components.WeekSelectorBottomSheet(
            currentWeek = currentWeek,
            totalWeeks = courseSettings.totalWeeks,
            onWeekSelected = { week -> viewModel.selectWeek(week) },
            onDismiss = { viewModel.hideWeekSelectorSheet() }
        )
    }
    
    // 导出对话框
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val allSemesters by viewModel.allSemesters.collectAsState()
    
    if (showExportDialog) {
        takagi.ru.saison.ui.components.ExportDialog(
            semesters = allSemesters,
            currentSemesterId = currentSemesterId,
            onDismiss = { viewModel.dismissExportDialog() },
            onExport = { options ->
                viewModel.dismissExportDialog()
                coroutineScope.launch {
                    // 保存导出选项
                    viewModel.saveExportOptions(options.semesterIds)
                    
                    // 获取建议的文件名
                    val fileName = if (options.semesterIds.size == 1) {
                        viewModel.getSuggestedFileName(options.semesterIds.first())
                    } else {
                        "课程表_${System.currentTimeMillis()}.json"
                    }
                    
                    // 启动文件选择器
                    exportLauncher.launch(fileName)
                }
            }
        )
    }
}

@Composable
fun CourseHeader(
    currentWeek: Int,
    totalWeeks: Int,
    onBackToCurrentWeek: () -> Unit,
    onSettingsClick: () -> Unit,
    onSemesterSettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "第 $currentWeek 周",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "共 $totalWeeks 周",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBackToCurrentWeek) {
                    Icon(Icons.Default.Today, contentDescription = "Today", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                IconButton(onClick = onImportClick) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Import", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
fun WeekHeader(
    startDate: LocalDate,
    currentDay: DayOfWeek?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 16.dp, bottom = 8.dp)
    ) {
        for (i in 0 until 7) {
            val date = startDate.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek
            val isToday = dayOfWeek == currentDay
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableGrid(
    coursesByDay: Map<DayOfWeek, List<Course>>,
    periods: List<CoursePeriod>,
    onCourseClick: (Long) -> Unit,
    onEmptyCellClick: (DayOfWeek, Int) -> Unit
) {
    val periodHeight = 64.dp
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Time Column
        Column(
            modifier = Modifier
                .width(40.dp)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
             periods.forEachIndexed { index, period ->
                 Box(
                     modifier = Modifier.height(periodHeight),
                     contentAlignment = Alignment.TopCenter
                 ) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Text(
                             text = (index + 1).toString(),
                             style = MaterialTheme.typography.labelMedium,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onSurface
                         )
                         Text(
                             text = period.startTime.toString(),
                             style = MaterialTheme.typography.labelSmall,
                             fontSize = 10.sp,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     }
                 }
             }
        }
        
        // Grid
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val dayWidth = maxWidth / 7
            
            // Background Lines
            Column {
                repeat(periods.size) {
                    Divider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(periodHeight - 1.dp))
                }
            }
            
            // Vertical Lines
            Row {
                repeat(7) {
                    Spacer(modifier = Modifier.width(dayWidth))
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
            }

            // Courses
            coursesByDay.forEach { (day, courses) ->
                val dayIndex = day.value - 1
                courses.forEach { course ->
                    val startPeriod = course.periodStart ?: 1
                    val endPeriod = course.periodEnd ?: 1
                    val duration = endPeriod - startPeriod + 1
                    
                    CourseCardItem(
                        course = course,
                        modifier = Modifier
                            .width(dayWidth)
                            .height(periodHeight * duration.toFloat())
                            .offset(x = dayWidth * dayIndex.toFloat(), y = periodHeight * (startPeriod - 1).toFloat())
                            .padding(2.dp),
                        onClick = { onCourseClick(course.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CourseCardItem(
    course: Course,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Generate a color based on course ID, cycling through theme containers
    // Using modulo 3 to avoid errorContainer (which is usually red)
    val colorIndex = (course.id.toInt() % 3).let { if (it < 0) -it else it }
    val containerColor = when (colorIndex) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (colorIndex) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (!course.location.isNullOrEmpty()) {
                Text(
                    text = course.location,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun getCurrentWeekNumber(semesterStartDate: LocalDate?): Int {
    if (semesterStartDate == null) {
        return 1
    }
    val today = LocalDate.now()
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(semesterStartDate, today)
    val weekNumber = (daysBetween / 7).toInt() + 1
    return weekNumber.coerceAtLeast(1)
}

