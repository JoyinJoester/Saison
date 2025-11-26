package takagi.ru.saison.data.repository

import androidx.glance.appwidget.updateAll
import takagi.ru.saison.data.local.database.dao.CourseDao
import takagi.ru.saison.domain.mapper.toDomain
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.WeekPattern
import takagi.ru.saison.ui.widget.model.WidgetCourse
import takagi.ru.saison.ui.widget.model.WidgetData
import takagi.ru.saison.util.WeekCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程小组件数据仓库实现
 */
@Singleton
class CourseWidgetRepositoryImpl @Inject constructor(
    private val courseDao: CourseDao,
    private val semesterRepository: SemesterRepository,
    private val weekCalculator: WeekCalculator,
    private val defaultSemesterInitializer: DefaultSemesterInitializer
) : CourseWidgetRepository {

    companion object {
        private const val TAG = "CourseWidgetRepository"
    }

    override suspend fun getWidgetData(today: LocalDate): WidgetData {
        val startTime = System.currentTimeMillis()
        android.util.Log.d(TAG, "Fetching widget data for date: $today")
        
        return try {
            // 确保至少存在一个学期
            try {
                defaultSemesterInitializer.ensureDefaultSemester()
                android.util.Log.d(TAG, "Default semester ensured")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to ensure default semester", e)
                // 继续执行，尝试获取现有学期
            }
            
            // 获取当前激活的学期
            val semester = semesterRepository.getDefaultSemester()
            
            if (semester == null) {
                android.util.Log.w(TAG, "No active semester found")
                return WidgetData.empty()
            }
            
            android.util.Log.d(TAG, "Active semester: ${semester.name}, startDate=${semester.startDate}, endDate=${semester.endDate}")

            // 检查今天是否在学期范围内
            if (today.isBefore(semester.startDate) || today.isAfter(semester.endDate)) {
                android.util.Log.w(TAG, "Today ($today) is outside semester range (${semester.startDate} to ${semester.endDate})")
                return WidgetData.empty()
            }

            // 计算当前周次
            val currentWeek = weekCalculator.calculateCurrentWeek(semester.startDate, today)
            android.util.Log.d(TAG, "Current week: $currentWeek")

            // 获取所有课程
            val allCourses = courseDao.getAllCoursesList()
                .map { it.toDomain() }
                .filter { it.semesterId == semester.id }
            
            android.util.Log.d(TAG, "Retrieved ${allCourses.size} courses for semester ${semester.id}")

            // 获取今天和明天的课程
            val tomorrow = today.plusDays(1)
            val todayCoursesRaw = filterCoursesForDate(allCourses, today, semester.startDate)
            val tomorrowCourses = filterCoursesForDate(allCourses, tomorrow, semester.startDate)

            // 格式化日期信息
            val dateFormatter = DateTimeFormatter.ofPattern("M.d", Locale.getDefault())
            val currentTime = java.time.LocalTime.now()
            
            // 过滤掉今天已经结束的课程
            val todayCourses = todayCoursesRaw.filter { course ->
                !isCourseFinished(course.endTime, currentTime)
            }
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d(TAG, "Widget data fetched in ${duration}ms: today=${todayCourses.size} courses, tomorrow=${tomorrowCourses.size} courses")
            
            WidgetData(
                semesterName = semester.name,
                date = today.format(dateFormatter),
                week = "第${currentWeek}周",
                dayOfWeek = getDayOfWeekString(today),
                todayCourses = todayCourses.map { it.toWidgetCourse(currentTime) },
                tomorrowCourses = tomorrowCourses.map { it.toWidgetCourse() },
                hasActiveSemester = true
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.e(TAG, "Failed to fetch widget data after ${duration}ms", e)
            WidgetData.empty()
        }
    }

    /**
     * 过滤指定日期的课程
     */
    private fun filterCoursesForDate(
        courses: List<Course>,
        date: LocalDate,
        semesterStartDate: LocalDate
    ): List<Course> {
        val currentWeek = weekCalculator.calculateCurrentWeek(semesterStartDate, date)
        
        android.util.Log.d("CourseWidget", "Filtering courses for date=$date, week=$currentWeek, total courses=${courses.size}")
        
        val filtered = courses
            .filter { course ->
                // 1. 检查星期几
                val dayMatches = course.dayOfWeek == date.dayOfWeek
                
                // 2. 检查周次模式
                val weekMatches = matchesWeekPattern(course, currentWeek)
                
                // 3. 检查日期范围（对于自定义周数的课程，跳过日期范围检查，只依赖周数）
                val dateMatches = if (course.weekPattern == WeekPattern.CUSTOM) {
                    // 自定义周数课程：只要周数匹配就显示，不检查日期范围
                    android.util.Log.d("CourseWidget", "CUSTOM course: ${course.name}, customWeeks=${course.customWeeks}, currentWeek=$currentWeek, weekMatches=$weekMatches")
                    true
                } else {
                    // 其他类型课程：需要检查日期范围
                    !date.isBefore(course.startDate) && !date.isAfter(course.endDate)
                }
                
                val result = dayMatches && weekMatches && dateMatches
                if (course.weekPattern == WeekPattern.CUSTOM) {
                    android.util.Log.d("CourseWidget", "Course ${course.name}: dayMatches=$dayMatches, weekMatches=$weekMatches, dateMatches=$dateMatches, result=$result")
                }
                result
            }
            .sortedBy { it.startTime }
        
        android.util.Log.d("CourseWidget", "Filtered ${filtered.size} courses for date=$date")
        return filtered
    }

    /**
     * 检查课程是否匹配当前周次
     */
    private fun matchesWeekPattern(course: Course, currentWeek: Int): Boolean {
        return when (course.weekPattern) {
            WeekPattern.ALL -> true
            WeekPattern.ODD -> currentWeek % 2 == 1
            WeekPattern.EVEN -> currentWeek % 2 == 0
            WeekPattern.A -> currentWeek % 2 == 1  // A周通常对应奇数周
            WeekPattern.B -> currentWeek % 2 == 0  // B周通常对应偶数周
            WeekPattern.CUSTOM -> course.customWeeks?.contains(currentWeek) == true
        }
    }

    /**
     * 获取星期几的中文字符串
     */
    private fun getDayOfWeekString(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }

    /**
     * 判断课程是否已经结束
     */
    private fun isCourseFinished(endTime: java.time.LocalTime, currentTime: java.time.LocalTime): Boolean {
        return currentTime.isAfter(endTime)
    }

    /**
     * 将 Course 转换为 WidgetCourse
     */
    private fun Course.toWidgetCourse(currentTime: java.time.LocalTime = java.time.LocalTime.now()): WidgetCourse {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        
        // 判断是否是当前正在上的课程
        val isCurrent = !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)
        
        return WidgetCourse(
            id = id,
            name = name,
            location = location,
            startTime = startTime.format(timeFormatter),
            endTime = endTime.format(timeFormatter),
            color = color,
            isCurrent = isCurrent
        )
    }
    
    override suspend fun updateWidget(context: android.content.Context) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d(TAG, "Updating course widgets...")
        
        try {
            // 需要注入CourseWidget实例来更新
            // 由于依赖注入的限制，我们使用反射或者通过Hilt EntryPoint
            val app = context.applicationContext as? takagi.ru.saison.SaisonApplication
            
            if (app != null) {
                val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                    app,
                    takagi.ru.saison.ui.widget.WidgetEntryPoints.CourseWidget::class.java
                )
                val courseWidget = entryPoint.courseWidget()
                courseWidget.updateAll(context)
                
                val duration = System.currentTimeMillis() - startTime
                android.util.Log.d(TAG, "Course widgets updated successfully in ${duration}ms")
            } else {
                android.util.Log.e(TAG, "Application context is not SaisonApplication")
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.e(TAG, "Failed to update course widgets after ${duration}ms", e)
        }
    }
}

// CourseWidgetEntryPoint moved to WidgetEntryPoints.kt for centralized management
