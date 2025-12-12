package takagi.ru.saison.ui.screens.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.domain.model.Frequency
import takagi.ru.saison.domain.model.Priority
import takagi.ru.saison.domain.model.RecurrenceRule
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.usecase.ParseNaturalLanguageUseCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val parseNaturalLanguageUseCase: ParseNaturalLanguageUseCase,
    private val preferencesManager: takagi.ru.saison.data.local.datastore.PreferencesManager
) : ViewModel() {
    
    private val _listUiState = MutableStateFlow(TaskListUiState())
    val listUiState: StateFlow<TaskListUiState> = _listUiState.asStateFlow()
    
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    
    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()
    
    private val _filterMode = MutableStateFlow(TaskFilterMode.ALL)
    val filterMode: StateFlow<TaskFilterMode> = _filterMode.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _groupMode = MutableStateFlow(GroupMode.DATE)
    val groupMode: StateFlow<GroupMode> = _groupMode.asStateFlow()
    
    private val _sortMode = MutableStateFlow(SortMode.SMART)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()
    
    private val _isCompletedExpanded = MutableStateFlow(false)
    val isCompletedExpanded: StateFlow<Boolean> = _isCompletedExpanded.asStateFlow()
    
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()
    
    private val _selectedTasks = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTasks: StateFlow<Set<Long>> = _selectedTasks.asStateFlow()
    
    // 当前选中的项目类型 - 任务页面始终显示 TASK
    private val _currentItemType = MutableStateFlow(takagi.ru.saison.domain.model.ItemType.TASK)
    val currentItemType: StateFlow<takagi.ru.saison.domain.model.ItemType> = _currentItemType.asStateFlow()

    // Tags
    val tags = taskRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTag = MutableStateFlow<takagi.ru.saison.domain.model.Tag?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    fun setSelectedTag(tag: takagi.ru.saison.domain.model.Tag?) {
        _selectedTag.value = tag
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            taskRepository.addTag(name)
        }
    }

    fun renameTag(tag: takagi.ru.saison.domain.model.Tag, newName: String) {
        viewModelScope.launch {
            taskRepository.renameTag(tag.id, newName)
        }
    }

    fun deleteTag(tag: takagi.ru.saison.domain.model.Tag) {
        viewModelScope.launch {
            taskRepository.deleteTag(tag.id)
            if (_selectedTag.value?.id == tag.id) {
                _selectedTag.value = null
            }
        }
    }
    
    // 任务列表 - 应用智能排序
    val tasks: StateFlow<List<Task>> = combine(
        filterMode,
        searchQuery,
        sortMode,
        selectedTag
    ) { filter, query, sort, tag ->
        FilterParams(filter, query, sort, tag)
    }.flatMapLatest { (filter, query, sort, tag) ->
        when {
            query.isNotEmpty() -> taskRepository.searchTasks(query)
            filter == TaskFilterMode.ALL -> taskRepository.getAllTasks()
            filter == TaskFilterMode.ACTIVE -> taskRepository.getIncompleteTasks()
            filter == TaskFilterMode.COMPLETED -> taskRepository.getCompletedTasks()
            filter == TaskFilterMode.FAVORITE -> taskRepository.getAllTasks().map { it.filter { task -> task.isFavorite } }
            else -> taskRepository.getAllTasks()
        }.map { taskList ->
            var result = taskList
            if (tag != null) {
                result = result.filter { it.category?.id == tag.id }
            }
            
            when (sort) {
                SortMode.SMART -> result.smartSort()
                SortMode.DATE_ASC -> result.sortedBy { it.dueDate }
                SortMode.DATE_DESC -> result.sortedByDescending { it.dueDate }
                SortMode.PRIORITY -> result.sortedByDescending { it.priority.ordinal }
                SortMode.TITLE -> result.sortedBy { it.title }
            }
        }
    }.catch { e ->
        _uiState.value = TaskUiState.Error(e.message ?: "Unknown error")
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class FilterParams(
        val filter: TaskFilterMode,
        val query: String,
        val sort: SortMode,
        val tag: takagi.ru.saison.domain.model.Tag?
    )
    
    // 统计信息
    val incompleteCount: StateFlow<Int> = taskRepository.getIncompleteTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val overdueCount: StateFlow<Int> = taskRepository.getOverdueTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    init {
        _uiState.value = TaskUiState.Success
        
        // 监听任务列表，首次加载完成后设置 isInitialLoading 为 false
        viewModelScope.launch {
            tasks.collect {
                _isInitialLoading.value = false
            }
        }
    }
    
    fun setFilterMode(mode: TaskFilterMode) {
        _filterMode.value = mode
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun createTaskFromNaturalLanguage(input: String) {
        viewModelScope.launch {
            try {
                val task = parseNaturalLanguageUseCase(input)
                taskRepository.insertTask(task)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to create task")
            }
        }
    }
    
    fun createTask(
        title: String,
        dueDateTime: LocalDateTime?,
        priority: Priority,
        tags: List<String>,
        repeatType: String = "不重复",
        reminderEnabled: Boolean = false,
        weekDays: Set<java.time.DayOfWeek> = emptySet(),
        categoryName: String? = null
    ) {
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                
                // 如果提供了分类名称，查找对应的Tag
                val categoryTag = if (!categoryName.isNullOrBlank()) {
                    taskRepository.getAllTags().firstOrNull()?.firstOrNull { it.name == categoryName }
                } else null
                
                // 创建重复规则
                val recurrenceRule = if (repeatType != "不重复") {
                    when (repeatType) {
                        "每天" -> RecurrenceRule(
                            frequency = Frequency.DAILY,
                            interval = 1
                        )
                        "每周" -> RecurrenceRule(
                            frequency = Frequency.WEEKLY,
                            interval = 1
                        )
                        "每月" -> RecurrenceRule(
                            frequency = Frequency.MONTHLY,
                            interval = 1
                        )
                        "每年" -> RecurrenceRule(
                            frequency = Frequency.YEARLY,
                            interval = 1
                        )
                        "自定义" -> if (weekDays.isNotEmpty()) {
                            RecurrenceRule(
                                frequency = Frequency.WEEKLY,
                                interval = 1,
                                byDay = weekDays.toList()
                            )
                        } else null
                        else -> null
                    }
                } else null
                
                // 设置提醒时间
                val reminderTime = if (reminderEnabled && dueDateTime != null) {
                    dueDateTime.minusHours(1) // 提前1小时提醒
                } else null
                
                val task = Task(
                    title = title,
                    priority = priority,
                    dueDate = dueDateTime,
                    repeatRule = recurrenceRule,
                    reminderTime = reminderTime,
                    category = categoryTag,
                    createdAt = now,
                    updatedAt = now
                )
                taskRepository.insertTask(task)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to create task")
            }
        }
    }
    
    fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletion(taskId, isCompleted)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to update task")
            }
        }
    }
    
    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to delete task")
            }
        }
    }
    
    fun deleteCompletedTasks() {
        viewModelScope.launch {
            try {
                taskRepository.deleteCompletedTasksOlderThan(0)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to delete completed tasks")
            }
        }
    }
    
    fun toggleFavorite(taskId: Long) {
        viewModelScope.launch {
            try {
                val task = tasks.value.find { it.id == taskId } ?: return@launch
                val updatedTask = task.copy(isFavorite = !task.isFavorite)
                taskRepository.updateTask(updatedTask)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to toggle favorite")
            }
        }
    }
    
    fun setGroupMode(mode: GroupMode) {
        _groupMode.value = mode
    }
    
    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }
    
    fun toggleCompletedExpanded() {
        _isCompletedExpanded.value = !_isCompletedExpanded.value
    }
    
    fun enterMultiSelectMode() {
        _isMultiSelectMode.value = true
    }
    

    
    companion object {
        private const val TAG = "TaskViewModel"
    }
    
    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedTasks.value = emptySet()
    }
    
    fun toggleTaskSelection(taskId: Long) {
        _selectedTasks.value = if (_selectedTasks.value.contains(taskId)) {
            _selectedTasks.value - taskId
        } else {
            _selectedTasks.value + taskId
        }
    }
    
    fun deleteSelectedTasks() {
        viewModelScope.launch {
            try {
                _selectedTasks.value.forEach { taskId ->
                    taskRepository.deleteTask(taskId)
                }
                exitMultiSelectMode()
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to delete tasks")
            }
        }
    }
    
    // 智能排序：收藏置顶、未完成在前、已完成置底
    // 未完成和已完成任务都按照：标星 > 优先级 > 日期 > 创建时间 排序
    private fun List<Task>.smartSort(): List<Task> {
        val (completed, incomplete) = partition { it.isCompleted }
        
        val sortedIncomplete = incomplete.sortedWith(
            compareByDescending<Task> { it.isFavorite }
                .thenByDescending { it.priority.ordinal }
                .thenBy { it.dueDate }
                .thenByDescending { it.createdAt }
        )
        
        val sortedCompleted = completed.sortedWith(
            compareByDescending<Task> { it.isFavorite }
                .thenByDescending { it.priority.ordinal }
                .thenBy { it.dueDate }
                .thenByDescending { it.createdAt }
        )
        
        return sortedIncomplete + sortedCompleted
    }
    
    // 按日期分组任务
    fun groupTasksByDate(tasks: List<Task>): Map<DateGroup, List<Task>> {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        
        // 计算本周的开始和结束日期（周一到周日）
        val dayOfWeek = today.dayOfWeek.value // 1=Monday, 7=Sunday
        val thisWeekStart = today.minusDays((dayOfWeek - 1).toLong()) // 本周周一
        val thisWeekEnd = thisWeekStart.plusDays(6) // 本周周日
        val nextWeekEnd = thisWeekEnd.plusDays(7) // 下周周日
        
        return tasks.groupBy { task ->
            when {
                task.dueDate == null -> DateGroup.NoDate
                task.dueDate.toLocalDate().isBefore(today) -> DateGroup.Overdue
                task.dueDate.toLocalDate() == today -> DateGroup.Today
                task.dueDate.toLocalDate() == today.plusDays(1) -> DateGroup.Tomorrow
                task.dueDate.toLocalDate().isAfter(today.plusDays(1)) && 
                    !task.dueDate.toLocalDate().isAfter(thisWeekEnd) -> DateGroup.ThisWeek
                task.dueDate.toLocalDate().isAfter(thisWeekEnd) && 
                    !task.dueDate.toLocalDate().isAfter(nextWeekEnd) -> DateGroup.NextWeek
                else -> DateGroup.Later
            }
        }.toSortedMap(compareBy { it.order })
    }
    
    // 按优先级分组任务
    fun groupTasksByPriority(tasks: List<Task>): Map<Priority, List<Task>> {
        return tasks.groupBy { it.priority }
            .toSortedMap(compareByDescending { it.ordinal })
    }
    
    // 计算完成率
    fun calculateCompletionRate(tasks: List<Task>): Float {
        if (tasks.isEmpty()) return 0f
        val completedCount = tasks.count { it.isCompleted }
        return completedCount.toFloat() / tasks.size
    }
    
    // 计算今日完成任务数
    fun calculateTodayCompletedCount(tasks: List<Task>): Int {
        val today = LocalDate.now()
        return tasks.count { task ->
            task.isCompleted && task.completedAt?.toLocalDate() == today
        }
    }
}

sealed class TaskUiState {
    object Loading : TaskUiState()
    object Success : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

enum class TaskFilterMode {
    ALL,
    ACTIVE,
    COMPLETED,
    FAVORITE
}

enum class GroupMode {
    NONE,       // 不分组
    DATE,       // 按日期
    PRIORITY,   // 按优先级
    TAG         // 按标签
}

enum class SortMode {
    SMART,      // 智能排序
    DATE_ASC,   // 日期升序
    DATE_DESC,  // 日期降序
    PRIORITY,   // 优先级
    TITLE       // 标题
}

sealed class DateGroup(val order: Int) {
    object Overdue : DateGroup(0)      // 逾期
    object Today : DateGroup(1)        // 今天
    object Tomorrow : DateGroup(2)     // 明天
    object ThisWeek : DateGroup(3)     // 本周
    object NextWeek : DateGroup(4)     // 下周
    object Later : DateGroup(5)        // 以后
    object NoDate : DateGroup(6)       // 无日期
    
    val displayName: String
        get() = when (this) {
            is Overdue -> "逾期"
            is Today -> "今天"
            is Tomorrow -> "明天"
            is ThisWeek -> "本周"
            is NextWeek -> "下周"
            is Later -> "以后"
            is NoDate -> "无日期"
        }
}

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val groupMode: GroupMode = GroupMode.DATE,
    val sortMode: SortMode = SortMode.SMART,
    val filterMode: TaskFilterMode = TaskFilterMode.ALL,
    val searchQuery: String = "",
    val isMultiSelectMode: Boolean = false,
    val selectedTasks: Set<Long> = emptySet(),
    val isCompletedExpanded: Boolean = false,
    val completionRate: Float = 0f,
    val incompleteCount: Int = 0,
    val overdueCount: Int = 0,
    val todayCompletedCount: Int = 0
)
