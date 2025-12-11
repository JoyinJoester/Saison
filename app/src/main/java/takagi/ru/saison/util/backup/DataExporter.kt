package takagi.ru.saison.util.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import takagi.ru.saison.domain.model.*
import takagi.ru.saison.domain.model.routine.RoutineTask
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DataExporter @Inject constructor(
    private val json: Json
) {
    
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    
    fun exportTasks(tasks: List<Task>): String {
        val dtos = tasks.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportCourses(courses: List<Course>): String {
        val dtos = courses.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportEvents(events: List<Event>): String {
        val dtos = events.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportRoutines(routines: List<RoutineTask>): String {
        val dtos = routines.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportSubscriptions(subscriptions: List<Subscription>): String {
        val dtos = subscriptions.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportPomodoroSessions(sessions: List<PomodoroSession>): String {
        val dtos = sessions.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportSemesters(semesters: List<Semester>): String {
        val dtos = semesters.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportCategories(categories: List<takagi.ru.saison.data.local.database.entities.CategoryEntity>): String {
        val dtos = categories.map { it.toBackupDto() }
        return json.encodeToString(dtos)
    }
    
    fun exportPreferences(preferences: Map<String, Any>): String {
        // 暂时返回空 JSON 对象，因为偏好设置导出尚未实现
        return "{}"
    }
    
    // DTO conversion functions
    private fun Task.toBackupDto() = TaskBackupDto(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate?.format(dateTimeFormatter),
        reminderTime = reminderTime?.format(dateTimeFormatter),
        location = location,
        priority = priority.value,
        isCompleted = isCompleted,
        completedAt = completedAt?.format(dateTimeFormatter),
        categoryId = category?.id,
        categoryName = category?.name,
        pomodoroCount = pomodoroCount,
        estimatedPomodoros = estimatedPomodoros,
        metronomeBpm = metronomeBpm,
        isFavorite = isFavorite,
        sortOrder = sortOrder,
        createdAt = createdAt.format(dateTimeFormatter),
        updatedAt = updatedAt.format(dateTimeFormatter)
    )
    
    private fun Course.toBackupDto() = CourseBackupDto(
        id = id,
        name = name,
        instructor = instructor,
        location = location,
        color = color,
        semesterId = semesterId,
        dayOfWeek = dayOfWeek.value,
        startTime = startTime.format(timeFormatter),
        endTime = endTime.format(timeFormatter),
        weekPattern = weekPattern.name,
        customWeeks = customWeeks,
        startDate = startDate.format(dateFormatter),
        endDate = endDate.format(dateFormatter),
        notificationMinutes = notificationMinutes,
        autoSilent = autoSilent,
        periodStart = periodStart,
        periodEnd = periodEnd,
        isCustomTime = isCustomTime,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun Event.toBackupDto() = EventBackupDto(
        id = id,
        title = title,
        description = description,
        eventDate = eventDate.format(dateTimeFormatter),
        category = category.name,
        isCompleted = isCompleted,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime?.format(dateTimeFormatter),
        createdAt = createdAt.format(dateTimeFormatter),
        updatedAt = updatedAt.format(dateTimeFormatter)
    )
    
    private fun RoutineTask.toBackupDto() = RoutineBackupDto(
        id = id,
        title = title,
        description = description,
        icon = icon,
        cycleType = cycleType.name,
        cycleConfig = cycleConfig.toString(),
        durationMinutes = durationMinutes,
        isActive = isActive,
        createdAt = createdAt.format(dateTimeFormatter),
        updatedAt = updatedAt.format(dateTimeFormatter)
    )
    
    private fun Subscription.toBackupDto() = SubscriptionBackupDto(
        id = id,
        name = name,
        description = description,
        price = price,
        currency = currency,
        billingCycle = billingCycle.name,
        startDate = startDate.format(dateFormatter),
        nextBillingDate = nextBillingDate.format(dateFormatter),
        reminderDaysBefore = reminderDaysBefore,
        isActive = isActive,
        category = category,
        icon = icon,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun PomodoroSession.toBackupDto() = PomodoroBackupDto(
        id = id,
        taskId = taskId,
        routineTaskId = routineTaskId,
        startTime = startTime,
        endTime = endTime,
        duration = duration,
        actualDuration = actualDuration,
        isCompleted = isCompleted,
        isBreak = isBreak,
        isLongBreak = isLongBreak,
        isEarlyFinish = isEarlyFinish,
        interruptions = interruptions,
        notes = notes
    )
    
    private fun Semester.toBackupDto() = SemesterBackupDto(
        id = id,
        name = name,
        startDate = startDate.format(dateFormatter),
        endDate = endDate.format(dateFormatter),
        totalWeeks = totalWeeks,
        isArchived = isArchived,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun takagi.ru.saison.data.local.database.entities.CategoryEntity.toBackupDto() = CategoryBackupDto(
        id = id,
        name = name,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Backup DTOs
@Serializable
data class TaskBackupDto(
    val id: Long,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val reminderTime: String?,
    val location: String?,
    val priority: Int,
    val isCompleted: Boolean,
    val completedAt: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val pomodoroCount: Int,
    val estimatedPomodoros: Int?,
    val metronomeBpm: Int?,
    val isFavorite: Boolean,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CourseBackupDto(
    val id: Long,
    val name: String,
    val instructor: String?,
    val location: String?,
    val color: Int,
    val semesterId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val weekPattern: String,
    val customWeeks: List<Int>?,
    val startDate: String,
    val endDate: String,
    val notificationMinutes: Int,
    val autoSilent: Boolean,
    val periodStart: Int?,
    val periodEnd: Int?,
    val isCustomTime: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class EventBackupDto(
    val id: Long,
    val title: String,
    val description: String?,
    val eventDate: String,
    val category: String,
    val isCompleted: Boolean,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class RoutineBackupDto(
    val id: Long,
    val title: String,
    val description: String?,
    val icon: String?,
    val cycleType: String,
    val cycleConfig: String,
    val durationMinutes: Int?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class SubscriptionBackupDto(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Double,
    val currency: String,
    val billingCycle: String,
    val startDate: String,
    val nextBillingDate: String,
    val reminderDaysBefore: Int,
    val isActive: Boolean,
    val category: String?,
    val icon: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class PomodoroBackupDto(
    val id: Long,
    val taskId: Long?,
    val routineTaskId: Long?,
    val startTime: Long,
    val endTime: Long?,
    val duration: Int,
    val actualDuration: Int?,
    val isCompleted: Boolean,
    val isBreak: Boolean,
    val isLongBreak: Boolean,
    val isEarlyFinish: Boolean,
    val interruptions: Int,
    val notes: String?
)

@Serializable
data class SemesterBackupDto(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val totalWeeks: Int,
    val isArchived: Boolean,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
