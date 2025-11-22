package takagi.ru.saison.ui.screens.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.WeekPattern
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val courseSettingsRepository: takagi.ru.saison.domain.repository.CourseSettingsRepository,
    private val weekCalculator: takagi.ru.saison.util.WeekCalculator,
    private val semesterRepository: takagi.ru.saison.data.repository.SemesterRepository,
    private val preferencesManager: takagi.ru.saison.data.local.datastore.PreferencesManager,
    private val exportCourseDataUseCase: takagi.ru.saison.domain.usecase.ExportCourseDataUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CourseUiState>(CourseUiState.Loading)
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()
    
    // 周数偏移量（0 = 当前周，-1 = 上一周，1 = 下一周）
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()
    
    // 周次选择器底部抽屉显示状态
    private val _showWeekSelectorSheet = MutableStateFlow(false)
    val showWeekSelectorSheet: StateFlow<Boolean> = _showWeekSelectorSheet.asStateFlow()
    
    // 课程设置
    val courseSettings: StateFlow<takagi.ru.saison.domain.model.CourseSettings> = 
        courseSettingsRepository.getSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = takagi.ru.saison.domain.model.CourseSettings()
            )
    
    // 当前周数 - 根据设置中的学期开始日期和偏移量计算
    val currentWeek: StateFlow<Int> = combine(
        courseSettings,
        _weekOffset
    ) { settings, offset ->
        val baseWeek = getCurrentWeekNumber(settings.semesterStartDate)
        val calculatedWeek = (baseWeek + offset).coerceIn(1, settings.totalWeeks)
        
        android.util.Log.d("CourseViewModel", "Week calculation: baseWeek=$baseWeek, offset=$offset, result=$calculatedWeek")
        
        calculatedWeek
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )
    
    // 计算的节次列表
    val periods: StateFlow<List<takagi.ru.saison.domain.model.CoursePeriod>> = 
        courseSettings.map { settings ->
            calculatePeriods(settings)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 当前学期ID
    private val currentSemesterId: Flow<Long?> = flow {
        val semesterId = preferencesManager.getCurrentSemesterId()
        emit(semesterId ?: run {
            // 如果没有保存的学期ID，获取默认学期或最新学期
            val defaultSemester = semesterRepository.getDefaultSemester()
                ?: semesterRepository.getLatestSemester()
            defaultSemester?.id
        })
    }
    
    // 公开的当前学期ID（用于UI访问）
    val currentSemesterIdState: StateFlow<Long?> = currentSemesterId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // 所有课程（过滤当前学期）
    val allCourses: StateFlow<List<Course>> = currentSemesterId
        .flatMapLatest { semesterId ->
            if (semesterId != null) {
                courseRepository.getCoursesBySemester(semesterId)
            } else {
                courseRepository.getAllCourses()
            }
        }
        .catch { e ->
            _uiState.value = CourseUiState.Error(e.message ?: "Unknown error")
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 按星期分组的课程（不进行周数过滤，由CourseScreen负责）
    // 这里只返回所有课程，周数过滤在HorizontalPager的每一页中进行
    val coursesByDay: StateFlow<Map<DayOfWeek, List<Course>>> = allCourses.map { courses ->
        courses.groupBy { it.dayOfWeek }
            .toSortedMap(compareBy { it.value })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    
    init {
        _uiState.value = CourseUiState.Success
        // 每次初始化时重置到当前周
        _weekOffset.value = 0
    }
    
    /**
     * 设置周数偏移量
     */
    fun setWeekOffset(offset: Int) {
        _weekOffset.value = offset
    }
    
    /**
     * 跳转到上一周
     */
    fun goToPreviousWeek() {
        _weekOffset.value -= 1
    }
    
    /**
     * 跳转到下一周
     */
    fun goToNextWeek() {
        _weekOffset.value += 1
    }
    
    /**
     * 回到当前周
     */
    fun goToCurrentWeek() {
        _weekOffset.value = 0
    }
    
    /**
     * 显示周次选择器底部抽屉
     */
    fun showWeekSelectorSheet() {
        _showWeekSelectorSheet.value = true
    }
    
    /**
     * 隐藏周次选择器底部抽屉
     */
    fun hideWeekSelectorSheet() {
        _showWeekSelectorSheet.value = false
    }
    
    /**
     * 选择指定周次
     */
    fun selectWeek(week: Int) {
        val settings = courseSettings.value
        val baseWeek = getCurrentWeekNumber(settings.semesterStartDate)
        _weekOffset.value = week - baseWeek
    }
    
    /**
     * 根据当前周数设置学期开始日期
     */
    fun setSemesterStartByCurrentWeek(currentWeekNumber: Int) {
        viewModelScope.launch {
            val startDate = calculateSemesterStartDate(currentWeekNumber)
            val updatedSettings = courseSettings.value.copy(
                semesterStartDate = startDate,
                updatedAt = System.currentTimeMillis()
            )
            updateSettings(updatedSettings)
        }
    }
    
    /**
     * 直接设置学期开始日期
     */
    fun setSemesterStartDate(date: LocalDate) {
        viewModelScope.launch {
            val updatedSettings = courseSettings.value.copy(
                semesterStartDate = date,
                updatedAt = System.currentTimeMillis()
            )
            updateSettings(updatedSettings)
        }
    }
    
    fun addCourse(course: Course) {
        viewModelScope.launch {
            try {
                courseRepository.insertCourse(course)
            } catch (e: Exception) {
                _uiState.value = CourseUiState.Error(e.message ?: "Failed to add course")
            }
        }
    }
    
    fun updateCourse(course: Course) {
        viewModelScope.launch {
            try {
                courseRepository.updateCourse(course)
            } catch (e: Exception) {
                _uiState.value = CourseUiState.Error(e.message ?: "Failed to update course")
            }
        }
    }
    
    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            try {
                courseRepository.deleteCourse(courseId)
            } catch (e: Exception) {
                _uiState.value = CourseUiState.Error(e.message ?: "Failed to delete course")
            }
        }
    }
    
    fun importCoursesFromOcr(imageUri: String) {
        viewModelScope.launch {
            try {
                // TODO: 实现 OCR 识别
                _uiState.value = CourseUiState.Error("OCR 功能尚未实现")
            } catch (e: Exception) {
                _uiState.value = CourseUiState.Error(e.message ?: "OCR failed")
            }
        }
    }
    
    /**
     * 判断课程在某周是否上课
     */
    fun isCourseActiveInWeek(course: Course, week: Int): Boolean {
        // 确保周数有效
        if (week < 1) {
            android.util.Log.w("CourseViewModel", "Invalid week number: $week for course ${course.name}")
            return false
        }
        
        val result = when (course.weekPattern) {
            WeekPattern.ALL -> {
                // 全周课程，总是显示
                true
            }
            WeekPattern.ODD -> {
                // 单周课程，只在奇数周显示
                week % 2 == 1
            }
            WeekPattern.EVEN -> {
                // 双周课程，只在偶数周显示
                week % 2 == 0
            }
            WeekPattern.CUSTOM -> {
                // 自定义周课程，检查是否在指定周数列表中
                val customWeeks = course.customWeeks
                if (customWeeks == null || customWeeks.isEmpty()) {
                    android.util.Log.w("CourseViewModel", "Course ${course.name} has CUSTOM pattern but no customWeeks")
                    false
                } else {
                    customWeeks.contains(week)
                }
            }
            WeekPattern.A -> {
                // A周模式 - 需要根据实际需求实现
                // 暂时简化处理，总是显示
                true
            }
            WeekPattern.B -> {
                // B周模式 - 需要根据实际需求实现
                // 暂时简化处理，总是显示
                true
            }
        }
        
        android.util.Log.d("CourseViewModel", "isCourseActiveInWeek: ${course.name}, pattern=${course.weekPattern}, week=$week, result=$result")
        
        return result
    }
    
    /**
     * 计算当前周数
     * @param semesterStartDate 学期开始日期，如果为null则使用年初作为起点
     */
    private fun getCurrentWeekNumber(semesterStartDate: LocalDate?): Int {
        if (semesterStartDate == null) {
            android.util.Log.w("CourseViewModel", "Semester start date not set, using year start")
        }
        
        val today = LocalDate.now()
        val startDate = semesterStartDate ?: LocalDate.of(today.year, 1, 1)
        val week = weekCalculator.calculateCurrentWeek(startDate, today)
        
        android.util.Log.d("CourseViewModel", "getCurrentWeekNumber: startDate=$startDate, today=$today, week=$week")
        
        return week
    }
    
    /**
     * 根据当前周数反推学期开始日期
     * @param currentWeekNumber 当前是第几周
     * @return 学期第一周的开始日期（周一）
     */
    private fun calculateSemesterStartDate(currentWeekNumber: Int): LocalDate {
        val today = LocalDate.now()
        // 计算需要回退的天数
        val daysToSubtract = (currentWeekNumber - 1) * 7L
        val semesterStart = today.minusDays(daysToSubtract)
        
        // 调整到周一
        val dayOfWeek = semesterStart.dayOfWeek.value // 1=Monday, 7=Sunday
        val daysFromMonday = dayOfWeek - 1
        return semesterStart.minusDays(daysFromMonday.toLong())
    }
    
    /**
     * 根据课程设置计算所有节次的时间
     * 使用PeriodGenerator生成节次列表
     */
    fun calculatePeriods(settings: takagi.ru.saison.domain.model.CourseSettings): List<takagi.ru.saison.domain.model.CoursePeriod> {
        val (periods, _) = takagi.ru.saison.util.PeriodGenerator.generatePeriods(
            totalPeriods = settings.totalPeriods,
            firstPeriodStartTime = settings.firstPeriodStartTime,
            periodDuration = settings.periodDuration,
            breakDuration = settings.breakDuration,
            lunchBreakAfterPeriod = settings.lunchBreakAfterPeriod,
            lunchBreakDuration = settings.lunchBreakDuration
        )
        return periods
    }
    
    /**
     * 根据节次编号获取节次信息
     */
    fun getPeriodByNumber(periodNumber: Int): takagi.ru.saison.domain.model.CoursePeriod? {
        return periods.value.find { it.periodNumber == periodNumber }
    }
    
    /**
     * 获取指定星期的可用节次（排除已有课程占用的节次）
     */
    fun getAvailablePeriods(dayOfWeek: DayOfWeek, excludeCourseId: Long? = null): List<takagi.ru.saison.domain.model.CoursePeriod> {
        val allPeriods = periods.value
        val coursesOnDay = allCourses.value
            .filter { it.dayOfWeek == dayOfWeek && it.id != excludeCourseId }
            .filter { !it.isCustomTime } // 只考虑按节次的课程
        
        // 获取已占用的节次范围
        val occupiedPeriods = mutableSetOf<Int>()
        coursesOnDay.forEach { course ->
            val start = course.periodStart ?: return@forEach
            val end = course.periodEnd ?: return@forEach
            for (i in start..end) {
                occupiedPeriods.add(i)
            }
        }
        
        // 返回未占用的节次
        return allPeriods.filter { it.periodNumber !in occupiedPeriods }
    }
    
    /**
     * 检测节次冲突
     * @return 冲突的课程列表，如果没有冲突则返回空列表
     */
    fun checkPeriodConflict(
        dayOfWeek: DayOfWeek,
        periodStart: Int,
        periodEnd: Int,
        excludeCourseId: Long? = null
    ): List<Course> {
        val coursesOnDay = allCourses.value
            .filter { it.dayOfWeek == dayOfWeek && it.id != excludeCourseId }
            .filter { !it.isCustomTime } // 只检查按节次的课程
        
        val newPeriodRange = periodStart..periodEnd
        
        return coursesOnDay.filter { course ->
            val start = course.periodStart ?: return@filter false
            val end = course.periodEnd ?: return@filter false
            val existingRange = start..end
            
            // 检查范围是否重叠
            newPeriodRange.any { it in existingRange } || existingRange.any { it in newPeriodRange }
        }
    }
    
    /**
     * 更新课程设置
     */
    fun updateSettings(settings: takagi.ru.saison.domain.model.CourseSettings) {
        viewModelScope.launch {
            try {
                courseSettingsRepository.updateSettings(settings)
            } catch (e: Exception) {
                _uiState.value = CourseUiState.Error(e.message ?: "Failed to update settings")
            }
        }
    }
    
    // ========== 导出功能 ==========
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    
    // 导出对话框显示状态
    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()
    
    // 导出选项
    private val _exportOptions = MutableStateFlow<ExportOptionsData?>(null)
    val exportOptions: StateFlow<ExportOptionsData?> = _exportOptions.asStateFlow()
    
    // 所有学期列表（用于导出对话框）
    val allSemesters = semesterRepository.getAllSemesters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * 显示导出对话框
     */
    fun showExportDialog() {
        _showExportDialog.value = true
    }
    
    /**
     * 隐藏导出对话框
     */
    fun dismissExportDialog() {
        _showExportDialog.value = false
    }
    
    /**
     * 保存导出选项
     */
    fun saveExportOptions(semesterIds: List<Long>) {
        _exportOptions.value = ExportOptionsData(semesterIds)
    }
    
    /**
     * 获取建议的文件名
     */
    suspend fun getSuggestedFileName(semesterId: Long): String {
        return try {
            val semester = semesterRepository.getSemesterByIdSync(semesterId)
            if (semester != null) {
                exportCourseDataUseCase.generateSuggestedFileName(semester.name)
            } else {
                "课程表_${System.currentTimeMillis()}.json"
            }
        } catch (e: Exception) {
            "课程表_${System.currentTimeMillis()}.json"
        }
    }
    
    /**
     * 导出课程表到用户选择的Uri
     * @param uri 用户通过SAF选择的保存位置
     * @param semesterIds 要导出的学期ID列表
     */
    suspend fun exportToUri(uri: android.net.Uri, semesterIds: List<Long>): Result<Unit> {
        return try {
            _exportState.value = ExportState.Loading
            
            // 使用新的JSON导出系统
            val result = exportCourseDataUseCase.exportToUri(uri, semesterIds)
            
            if (result.isSuccess) {
                _exportState.value = ExportState.Success(uri)
            } else {
                _exportState.value = ExportState.Error(result.exceptionOrNull()?.message ?: "导出失败")
            }
            
            result
        } catch (e: Exception) {
            _exportState.value = ExportState.Error(e.message ?: "导出失败")
            Result.failure(e)
        }
    }
    
    /**
     * 重置导出状态
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
        _exportOptions.value = null
    }
    
    // ========== 网格视图增强功能 ==========
    
    // 当前节次(根据当前时间计算)
    private val _currentPeriod = MutableStateFlow<Int?>(null)
    val currentPeriod: StateFlow<Int?> = _currentPeriod.asStateFlow()
    
    // 当前星期
    private val _currentDay = MutableStateFlow<DayOfWeek>(LocalDate.now().dayOfWeek)
    val currentDay: StateFlow<DayOfWeek> = _currentDay.asStateFlow()
    
    /**
     * 更新当前节次
     * 根据当前时间和节次列表计算当前正在进行的节次
     */
    fun updateCurrentPeriod() {
        val now = java.time.LocalTime.now()
        val currentPeriods = periods.value
        
        val period = currentPeriods.find { 
            now >= it.startTime && now < it.endTime 
        }
        
        _currentPeriod.value = period?.periodNumber
        _currentDay.value = LocalDate.now().dayOfWeek
    }
    
    /**
     * 获取指定星期和节次的已有课程
     * 
     * @param day 星期
     * @param periodNumber 节次编号
     * @return 该时间段的课程列表
     */
    fun getCoursesAt(day: DayOfWeek, periodNumber: Int): List<Course> {
        return coursesByDay.value[day]?.filter { course ->
            val start = course.periodStart ?: return@filter false
            val end = course.periodEnd ?: return@filter false
            periodNumber in start..end
        } ?: emptyList()
    }
    
    /**
     * 检测课程冲突
     * 
     * @param day 星期
     * @param periodStart 开始节次
     * @param periodEnd 结束节次
     * @param excludeCourseId 要排除的课程ID(编辑时使用)
     * @return 冲突的课程列表
     */
    fun detectConflict(
        day: DayOfWeek,
        periodStart: Int,
        periodEnd: Int,
        excludeCourseId: Long? = null
    ): List<Course> {
        return coursesByDay.value[day]?.filter { course ->
            if (course.id == excludeCourseId) return@filter false
            
            val start = course.periodStart ?: return@filter false
            val end = course.periodEnd ?: return@filter false
            
            // 检测时间段是否重叠
            !(periodEnd < start || periodStart > end)
        } ?: emptyList()
    }
}

sealed class CourseUiState {
    object Loading : CourseUiState()
    object Success : CourseUiState()
    data class Error(val message: String) : CourseUiState()
}

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val uri: android.net.Uri) : ExportState()
    data class Error(val message: String) : ExportState()
}

/**
 * 导出选项数据
 */
data class ExportOptionsData(
    val semesterIds: List<Long>
)
