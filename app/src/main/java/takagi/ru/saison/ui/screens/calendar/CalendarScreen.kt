package takagi.ru.saison.ui.screens.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Event
import takagi.ru.saison.domain.model.Task
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onCourseClick: (Long) -> Unit = {},
    onTaskClick: (Long) -> Unit = {},
    onEventClick: (Long) -> Unit = {},
    onRoutineClick: (Long) -> Unit = {},
    onSubscriptionClick: (Long) -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val events by viewModel.events.collectAsState()
    val daysWithCourses by viewModel.daysWithCourses.collectAsState()
    val datesWithTasksOrEvents by viewModel.datesWithTasksOrEvents.collectAsState()
    val todayRoutineTasks by viewModel.todayRoutineTasks.collectAsState()
    val upcomingSubscriptions by viewModel.upcomingSubscriptions.collectAsState()
    
    val hasIndicator = remember(daysWithCourses, datesWithTasksOrEvents) {
        { date: LocalDate ->
            daysWithCourses.contains(date.dayOfWeek) || datesWithTasksOrEvents.contains(date)
        }
    }

    // Pager State
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    
    // Base month for pager calculations to ensure consistency
    val baseMonth = remember { YearMonth.from(LocalDate.now()) }
    
    // Sync Pager with Current Month
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var isMonthViewExpanded by remember { mutableStateOf(true) }

    // Update currentMonth when page changes
    LaunchedEffect(pagerState.currentPage) {
        val pageDiff = pagerState.currentPage - initialPage
        if (viewMode == CalendarViewMode.MONTH && isMonthViewExpanded) {
             val newMonth = baseMonth.plusMonths(pageDiff.toLong())
             if (newMonth != currentMonth) {
                 currentMonth = newMonth
             }
        }
    }

    // Update Pager when currentMonth changes (e.g. via buttons)
    LaunchedEffect(currentMonth) {
        if (viewMode == CalendarViewMode.MONTH && isMonthViewExpanded) {
            val monthsDiff = java.time.temporal.ChronoUnit.MONTHS.between(baseMonth, currentMonth).toInt()
            val targetPage = initialPage + monthsDiff
            if (pagerState.currentPage != targetPage) {
                // Use scrollToPage for large jumps (like year changes) to avoid rendering issues
                if (abs(pagerState.currentPage - targetPage) > 1) {
                    pagerState.scrollToPage(targetPage)
                } else {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            // Custom Calendar Header
            if (viewMode == CalendarViewMode.MONTH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Month and Year
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }
                    
                    Column(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (abs(offsetX) > abs(offsetY)) {
                                            if (offsetX > 50) {
                                                // Swipe Right -> Previous Month
                                                currentMonth = currentMonth.minusMonths(1)
                                                if (!isMonthViewExpanded) {
                                                    viewModel.onDateSelected(currentMonth.atDay(1))
                                                }
                                            } else if (offsetX < -50) {
                                                // Swipe Left -> Next Month
                                                currentMonth = currentMonth.plusMonths(1)
                                                if (!isMonthViewExpanded) {
                                                    viewModel.onDateSelected(currentMonth.atDay(1))
                                                }
                                            }
                                        } else {
                                            if (offsetY < -50) {
                                                // Swipe Up -> Next Year
                                                currentMonth = currentMonth.plusYears(1)
                                                if (!isMonthViewExpanded) {
                                                    viewModel.onDateSelected(currentMonth.atDay(1))
                                                }
                                            } else if (offsetY > 50) {
                                                // Swipe Down -> Previous Year
                                                currentMonth = currentMonth.minusYears(1)
                                                if (!isMonthViewExpanded) {
                                                    viewModel.onDateSelected(currentMonth.atDay(1))
                                                }
                                            }
                                        }
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Right: Control Buttons Group
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { 
                                currentMonth = currentMonth.minusMonths(1)
                                if (!isMonthViewExpanded) {
                                    viewModel.onDateSelected(currentMonth.atDay(1))
                                }
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }
                            
                            IconButton(onClick = { 
                                currentMonth = currentMonth.plusMonths(1)
                                if (!isMonthViewExpanded) {
                                    viewModel.onDateSelected(currentMonth.atDay(1))
                                }
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                            
                            IconButton(onClick = { isMonthViewExpanded = !isMonthViewExpanded }) {
                                Icon(
                                    imageVector = if (isMonthViewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isMonthViewExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }
                }
            }

            // Calendar Content Container (Card Style)
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .animateContentSize(), // Animate size changes (collapse/expand)
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    // Weekday headers inside the card
                    if (viewMode == CalendarViewMode.MONTH) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            // Start from Monday (1) to Sunday (7)
                            val daysOfWeek = (1..7).map { dayValue ->
                                java.time.DayOfWeek.of(dayValue).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            }
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when (viewMode) {
                        CalendarViewMode.MONTH -> {
                            if (isMonthViewExpanded) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth()
                                ) { page ->
                                    val pageDiff = page - initialPage
                                    val month = baseMonth.plusMonths(pageDiff.toLong())
                                    
                                    CalendarView(
                                        currentMonth = month,
                                        selectedDate = selectedDate,
                                        onDateSelected = { date ->
                                            viewModel.onDateSelected(date)
                                        },
                                        showHeaders = false, // Headers are now outside
                                        hasIndicator = hasIndicator
                                    )
                                }
                            } else {
                                // Collapsed View (Week View)
                                val currentWeekPage = remember(selectedDate) {
                                    val weeksDiff = java.time.temporal.ChronoUnit.WEEKS.between(
                                        LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                                        selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                    ).toInt()
                                    initialPage + weeksDiff
                                }
                                
                                val weekPagerState = rememberPagerState(
                                    initialPage = currentWeekPage,
                                    pageCount = { Int.MAX_VALUE }
                                )
                                
                                HorizontalPager(
                                    state = weekPagerState,
                                    modifier = Modifier.fillMaxWidth()
                                ) { page ->
                                    val pageDiff = page - initialPage
                                    val baseDate = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                    val startOfWeek = baseDate.plusWeeks(pageDiff.toLong())
                                    
                                    val isSelectedInThisWeek = selectedDate.isAfter(startOfWeek.minusDays(1)) && selectedDate.isBefore(startOfWeek.plusDays(7))
                                    
                                    WeekView(
                                        selectedDate = if (isSelectedInThisWeek) selectedDate else startOfWeek,
                                        onDateSelected = { viewModel.onDateSelected(it) },
                                        forceShowWeekOf = startOfWeek,
                                        showHeaders = false,
                                        hasIndicator = hasIndicator
                                    )
                                }
                            }
                        }
                        CalendarViewMode.WEEK -> WeekView(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.onDateSelected(it) },
                            hasIndicator = hasIndicator
                        )
                        CalendarViewMode.DAY -> DayView(
                            selectedDate = selectedDate,
                            onDateSelected = { viewModel.onDateSelected(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule List
            ScheduleList(
                courses = courses,
                tasks = tasks,
                events = events,
                routineTasks = todayRoutineTasks,
                subscriptions = upcomingSubscriptions,
                selectedDate = selectedDate,
                onCourseClick = onCourseClick,
                onTaskClick = onTaskClick,
                onEventClick = onEventClick,
                onRoutineClick = onRoutineClick,
                onSubscriptionClick = onSubscriptionClick,
                onRoutineCheckIn = { taskId ->
                    viewModel.checkInRoutineTask(taskId)
                },
                onTaskToggleComplete = { taskId ->
                    viewModel.toggleTaskCompletion(taskId)
                }
            )
        }
    }
}

@Composable
fun WeekView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    forceShowWeekOf: LocalDate? = null,
    showHeaders: Boolean = true,
    hasIndicator: (LocalDate) -> Boolean = { false }
) {
    val dateForWeek = forceShowWeekOf ?: selectedDate
    // Assuming Monday start, calculate start of week
    val startOfWeek = dateForWeek.minusDays(dateForWeek.dayOfWeek.value.toLong() - 1)
    
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()
            
            Column(
                modifier = Modifier.weight(1f).clickable { onDateSelected(date) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showHeaders) {
                    Text(text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (hasIndicator(date)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(selectedDate.minusDays(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
        }
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.headlineSmall
        )
        IconButton(onClick = { onDateSelected(selectedDate.plusDays(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    showHeaders: Boolean = true,
    hasIndicator: (LocalDate) -> Boolean = { false }
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
    
    // Adjust to start from Monday (1) or Sunday (7 -> 0)
    // Let's assume Monday start for now.
    val emptySlots = firstDayOfMonth - 1

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Weekday headers
        if (showHeaders) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Start from Monday (1) to Sunday (7)
                val daysOfWeek = (1..7).map { dayValue ->
                    java.time.DayOfWeek.of(dayValue).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        val totalSlots = emptySlots + daysInMonth
        val rows = (totalSlots + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    if (index < emptySlots || index >= totalSlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val day = index - emptySlots + 1
                        val date = currentMonth.atDay(day)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isToday) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (hasIndicator(date)) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else if (isToday) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleList(
    courses: List<Course>,
    tasks: List<Task>,
    events: List<Event>,
    routineTasks: List<takagi.ru.saison.domain.model.routine.RoutineTaskWithStats>,
    subscriptions: List<takagi.ru.saison.data.local.database.entities.SubscriptionEntity>,
    selectedDate: LocalDate,
    onCourseClick: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onEventClick: (Long) -> Unit,
    onRoutineClick: (Long) -> Unit,
    onSubscriptionClick: (Long) -> Unit,
    onRoutineCheckIn: (Long) -> Unit,
    onTaskToggleComplete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (courses.isNotEmpty()) {
            item {
                SectionHeader(title = "Courses")
            }
            items(courses) { course ->
                CourseItem(course, onClick = { onCourseClick(course.id) })
            }
        }

        if (tasks.isNotEmpty()) {
            item {
                SectionHeader(title = "Tasks")
            }
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    onToggleComplete = { onTaskToggleComplete(task.id) }
                )
            }
        }

        if (events.isNotEmpty()) {
            item {
                SectionHeader(title = "Events")
            }
            items(events) { event ->
                EventItem(event, onClick = { onEventClick(event.id) })
            }
        }
        
        // 日程打卡任务
        if (routineTasks.isNotEmpty()) {
            item {
                SectionHeader(title = "日程打卡")
            }
            items(routineTasks) { taskWithStats ->
                takagi.ru.saison.ui.components.RoutineCardCompact(
                    taskWithStats = taskWithStats,
                    onCheckIn = { onRoutineCheckIn(taskWithStats.task.id) },
                    onClick = { onRoutineClick(taskWithStats.task.id) }
                )
            }
        }
        
        // 即将续费的订阅
        if (subscriptions.isNotEmpty()) {
            item {
                SectionHeader(title = "即将续费")
            }
            items(subscriptions) { subscription ->
                takagi.ru.saison.ui.components.SubscriptionItemCompact(
                    subscription = subscription,
                    selectedDate = selectedDate,
                    onClick = { onSubscriptionClick(subscription.id) }
                )
            }
        }
        
        if (courses.isEmpty() && tasks.isEmpty() && events.isEmpty() && routineTasks.isEmpty() && subscriptions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No schedule for today", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun CourseItem(course: Course, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // 如果有颜色属性，可以显示一个小圆点或者边框，这里暂时只用大色块
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${course.startTime} - ${course.endTime}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            course.location?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                task.dueDate?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    Text(
                        text = "Due: ${it.format(formatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun EventItem(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                Text(
                    text = event.eventDate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!event.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }
    }
}
