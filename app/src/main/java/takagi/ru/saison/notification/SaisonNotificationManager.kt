package takagi.ru.saison.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.data.local.datastore.PreferencesManager
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.model.notification.NotificationData
import takagi.ru.saison.domain.model.notification.NotificationPriority
import takagi.ru.saison.domain.model.notification.NotificationType
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saison 通知管理器
 * 统一管理所有通知功能
 */
@Singleton
class SaisonNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelManager: NotificationChannelManager,
    private val permissionManager: NotificationPermissionManager,
    private val notificationBuilder: NotificationBuilder,
    private val scheduler: NotificationScheduler,
    private val preferencesManager: PreferencesManager
) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    /**
     * 初始化通知系统
     */
    fun initialize() {
        channelManager.initializeChannels()
    }
    
    /**
     * 调度任务提醒
     * 
     * @param task 任务对象
     */
    suspend fun scheduleTaskReminder(task: Task) {
        // 检查是否启用任务提醒
        val taskRemindersEnabled = preferencesManager.taskRemindersEnabled.first()
        if (!taskRemindersEnabled) return
        
        // 优先使用用户设置的提醒时间，没有则默认截止前1小时
        val dueDate = task.dueDate
        val reminderTime = task.reminderTime ?: dueDate?.minusHours(1) ?: return
        val triggerTime = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // 如果时间已过，不调度
        if (triggerTime <= System.currentTimeMillis()) return
        
        val notificationData = NotificationData(
            id = "task_${task.id}",
            type = NotificationType.TASK_REMINDER,
            title = "任务提醒",
            content = task.title,
            triggerTime = triggerTime,
            itemId = task.id,
            priority = when (task.priority) {
                takagi.ru.saison.domain.model.Priority.URGENT -> NotificationPriority.MAX
                takagi.ru.saison.domain.model.Priority.HIGH -> NotificationPriority.HIGH
                takagi.ru.saison.domain.model.Priority.MEDIUM -> NotificationPriority.DEFAULT
                takagi.ru.saison.domain.model.Priority.LOW -> NotificationPriority.LOW
            }
        )
        
        scheduler.scheduleOneTime(notificationData.id, triggerTime, notificationData)
    }
    
    /**
     * 取消任务提醒
     * 
     * @param taskId 任务ID
     */
    fun cancelTaskReminder(taskId: Long) {
        val notificationId = "task_$taskId"
        scheduler.cancel(notificationId)
        notificationManager.cancel(notificationId.hashCode())
        notificationManager.cancel(taskId.toInt())
    }
    
    /**
     * 调度课程提醒
     * 
     * @param course 课程对象
     */
    suspend fun scheduleCourseReminder(course: Course) {
        // 检查是否启用课程提醒
        val courseRemindersEnabled = preferencesManager.courseRemindersEnabled.first()
        if (!courseRemindersEnabled) return
        
        // 计算下次上课时间
        val now = LocalDateTime.now()
        val nextClassTime = calculateNextClassTime(course, now) ?: return
        
        // 计算提醒时间（上课前10分钟）
        val reminderTime = nextClassTime.minusMinutes(course.notificationMinutes.toLong())
        val triggerTime = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // 如果时间已过，不调度
        if (triggerTime <= System.currentTimeMillis()) return
        
        val notificationData = NotificationData(
            id = "course_${course.id}",
            type = NotificationType.COURSE_REMINDER,
            title = "课程提醒",
            content = course.name,
            triggerTime = triggerTime,
            itemId = course.id,
            priority = NotificationPriority.HIGH
        )
        
        scheduler.scheduleOneTime(notificationData.id, triggerTime, notificationData)
    }
    
    /**
     * 取消课程提醒
     * 
     * @param courseId 课程ID
     */
    fun cancelCourseReminder(courseId: Long) {
        val notificationId = "course_$courseId"
        scheduler.cancel(notificationId)
        notificationManager.cancel(notificationId.hashCode())
        notificationManager.cancel(courseId.toInt())
    }
    
    /**
     * 调度番茄钟提醒
     * 
     * @param sessionId 会话ID
     * @param isWorkEnd 是否为工作时段结束
     * @param triggerTime 触发时间
     */
    suspend fun schedulePomodoroReminder(sessionId: Long, isWorkEnd: Boolean, triggerTime: Long) {
        // 检查是否启用番茄钟提醒
        val pomodoroRemindersEnabled = preferencesManager.pomodoroRemindersEnabled.first()
        if (!pomodoroRemindersEnabled) return
        
        val type = if (isWorkEnd) NotificationType.POMODORO_WORK_END else NotificationType.POMODORO_BREAK_END
        
        val notificationData = NotificationData(
            id = "pomodoro_${sessionId}_${if (isWorkEnd) "work" else "break"}",
            type = type,
            title = if (isWorkEnd) "工作时段结束" else "休息时段结束",
            content = if (isWorkEnd) "该休息一下了！" else "休息结束，继续加油！",
            triggerTime = triggerTime,
            itemId = sessionId,
            priority = NotificationPriority.MAX
        )
        
        scheduler.scheduleOneTime(notificationData.id, triggerTime, notificationData)
    }
    
    /**
     * 取消番茄钟提醒
     * 
     * @param sessionId 会话ID
     */
    fun cancelPomodoroReminder(sessionId: Long) {
        val workNotificationId = "pomodoro_${sessionId}_work"
        val breakNotificationId = "pomodoro_${sessionId}_break"
        scheduler.cancel(workNotificationId)
        scheduler.cancel(breakNotificationId)
        notificationManager.cancel(workNotificationId.hashCode())
        notificationManager.cancel(breakNotificationId.hashCode())
        notificationManager.cancel(sessionId.toInt())
    }
    
    /**
     * 显示通知
     * 
     * @param notificationId 通知ID
     * @param notification 通知对象
     */
    fun showNotification(notificationId: Int, notification: android.app.Notification) {
        if (permissionManager.checkNotificationPermission()) {
            notificationManager.notify(notificationId, notification)
        }
    }
    
    /**
     * 取消所有任务提醒
     */
    fun cancelAllTaskReminders() {
        scheduler.cancelAll(NotificationType.TASK_REMINDER)
    }
    
    /**
     * 取消所有课程提醒
     */
    fun cancelAllCourseReminders() {
        scheduler.cancelAll(NotificationType.COURSE_REMINDER)
    }
    
    /**
     * 计算下次上课时间
     */
    private fun calculateNextClassTime(course: Course, from: LocalDateTime): LocalDateTime? {
        // 简化实现：返回下一个匹配的上课时间
        var candidate = from.toLocalDate().atTime(course.startTime)
        
        // 如果今天的时间已过，从明天开始
        if (candidate.isBefore(from)) {
            candidate = candidate.plusDays(1)
        }
        
        // 查找下一个匹配星期几的日期
        while (candidate.dayOfWeek != course.dayOfWeek) {
            candidate = candidate.plusDays(1)
        }
        
        // 检查是否在课程日期范围内
        if (candidate.toLocalDate().isAfter(course.endDate)) {
            return null
        }
        
        return candidate
    }
}
