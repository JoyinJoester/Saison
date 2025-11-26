package takagi.ru.saison.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.repository.EventRepository
import takagi.ru.saison.domain.model.Event
import takagi.ru.saison.domain.model.WeekPattern
import takagi.ru.saison.domain.repository.CourseSettingsRepository
import takagi.ru.saison.util.WeekCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val eventRepository: EventRepository,
    private val courseSettingsRepository: CourseSettingsRepository,
    private val weekCalculator: WeekCalculator,
    private val routineRepository: takagi.ru.saison.data.repository.RoutineRepository,
    private val subscriptionRepository: takagi.ru.saison.data.repository.SubscriptionRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val courses: StateFlow<List<Course>> = combine(
        _selectedDate,
        courseSettingsRepository.getSettings()
    ) { date, settings ->
        Pair(date, settings)
    }.flatMapLatest { (date, settings) ->
        courseRepository.getCoursesByDay(date.dayOfWeek)
            .map { rawCourses ->
                // Calculate week number (use current date as fallback if semesterStartDate is null)
                val weekNumber = weekCalculator.calculateCurrentWeek(
                    settings.semesterStartDate ?: LocalDate.now(), 
                    date
                )
                
                // Filter by date range AND week pattern
                val activeCourses = rawCourses.filter { course ->
                    // 检查周数匹配
                    val isWeekMatch = when (course.weekPattern) {
                        WeekPattern.ALL -> true
                        WeekPattern.ODD -> weekNumber % 2 != 0
                        WeekPattern.EVEN -> weekNumber % 2 == 0
                        WeekPattern.A -> weekNumber % 2 != 0 // Treat A as Odd
                        WeekPattern.B -> weekNumber % 2 == 0 // Treat B as Even
                        WeekPattern.CUSTOM -> course.customWeeks?.contains(weekNumber) == true
                    }
                    
                    // 检查日期范围（对于自定义周数的课程，跳过日期范围检查，只依赖周数）
                    val isDateInRange = if (course.weekPattern == WeekPattern.CUSTOM) {
                        // 自定义周数课程：只要周数匹配就显示，不检查日期范围
                        true
                    } else {
                        // 其他类型课程：需要检查日期范围
                        !date.isBefore(course.startDate) && !date.isAfter(course.endDate)
                    }
                    
                    isDateInRange && isWeekMatch
                }
                mergeAdjacentCourses(activeCourses)
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun mergeAdjacentCourses(courses: List<Course>): List<Course> {
        if (courses.isEmpty()) return emptyList()
        
        // Sort by start time
        val sorted = courses.sortedWith(compareBy({ it.startTime }, { it.name }))
        val merged = mutableListOf<Course>()
        
        var current = sorted[0]
        
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            
            // Check if same course (name, location, instructor)
            val isSameCourse = current.name == next.name && 
                               current.location == next.location && 
                               current.instructor == next.instructor
            
            if (isSameCourse) {
                // Check adjacency
                // Case 1: Using periods (if available)
                val isPeriodAdjacent = if (current.periodEnd != null && next.periodStart != null) {
                    current.periodEnd!! + 1 == next.periodStart!!
                } else false
                
                // Case 2: Using time (gap less than 30 mins)
                // Calculate minutes between current.endTime and next.startTime
                val currentEndMinutes = current.endTime.hour * 60 + current.endTime.minute
                val nextStartMinutes = next.startTime.hour * 60 + next.startTime.minute
                val gapMinutes = nextStartMinutes - currentEndMinutes
                
                val isTimeAdjacent = gapMinutes in 0..30 // Allow up to 30 min break
                
                if (isPeriodAdjacent || isTimeAdjacent) {
                    // Merge: extend current course to end of next course
                    current = current.copy(
                        endTime = next.endTime,
                        periodEnd = next.periodEnd
                    )
                    continue
                }
            }
            
            merged.add(current)
            current = next
        }
        merged.add(current)
        
        return merged
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        taskRepository.getTasksByDateRange(startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<Event>> = _selectedDate.flatMapLatest { date ->
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        eventRepository.getEventsByDateRange(startOfDay, endOfDay)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val daysWithCourses: StateFlow<Set<java.time.DayOfWeek>> = courseRepository.getAllCourses()
        .map { courses -> courses.map { it.dayOfWeek }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val datesWithTasksOrEvents: StateFlow<Set<LocalDate>> = combine(
        taskRepository.getAllTasks(),
        eventRepository.getAllEvents()
    ) { tasks, events ->
        val taskDates = tasks.mapNotNull { it.dueDate?.toLocalDate() }.toSet()
        val eventDates = events.map { it.eventDate.toLocalDate() }.toSet()
        taskDates + eventDates
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // 获取当天活跃的日程任务
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayRoutineTasks: StateFlow<List<takagi.ru.saison.domain.model.routine.RoutineTaskWithStats>> = 
        _selectedDate.flatMapLatest { date ->
            routineRepository.getRoutineTasksWithStats().map { allTasks ->
                // 过滤出当天活跃的任务
                val cycleCalculator = takagi.ru.saison.util.CycleCalculator()
                allTasks.filter { taskWithStats ->
                    cycleCalculator.isInActiveCycle(taskWithStats.task, date)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 获取即将到来的订阅（未来7天内）
    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingSubscriptions: StateFlow<List<takagi.ru.saison.data.local.database.entities.SubscriptionEntity>> = 
        _selectedDate.flatMapLatest { date ->
            val startTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = date.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            subscriptionRepository.getActiveSubscriptions().map { subscriptions ->
                subscriptions.filter { subscription ->
                    subscription.nextRenewalDate in startTimestamp..endTimestamp
                }.sortedBy { it.nextRenewalDate }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }
    
    fun checkInRoutineTask(taskId: Long) {
        viewModelScope.launch {
            try {
                routineRepository.checkIn(taskId)
                // 刷新日程任务数据以更新UI
                refreshTodayRoutineTasks()
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("CalendarViewModel", "Failed to check in routine task", e)
            }
        }
    }
    
    /**
     * 切换任务完成状态并刷新UI
     */
    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    taskRepository.toggleTaskCompletion(taskId, !task.isCompleted)
                    // 刷新任务数据以更新UI
                    refreshTasks()
                }
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("CalendarViewModel", "Failed to toggle task completion", e)
            }
        }
    }
    
    /**
     * 手动刷新日程任务数据
     */
    private fun refreshTodayRoutineTasks() {
        // 触发 todayRoutineTasks 的重新计算
        // 通过重新发射相同的日期值来触发 flatMapLatest
        _selectedDate.value = _selectedDate.value
    }
    
    /**
     * 手动刷新任务数据
     */
    private fun refreshTasks() {
        // 触发 tasks 的重新计算
        // 通过重新发射相同的日期值来触发 flatMapLatest
        _selectedDate.value = _selectedDate.value
    }
}

enum class CalendarViewMode {
    MONTH, WEEK, DAY
}
