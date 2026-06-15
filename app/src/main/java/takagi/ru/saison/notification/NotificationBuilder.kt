package takagi.ru.saison.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.model.notification.NotificationChannels
import takagi.ru.saison.domain.model.notification.NotificationPriority
import takagi.ru.saison.domain.model.notification.NotificationType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知构建器
 * 负责构建各种类型的通知
 */
@Singleton
class NotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
    
    /**
     * 构建任务提醒通知
     * 
     * @param task 任务对象
     * @return 通知对象
     */
    fun buildTaskReminder(task: Task): Notification {
        val title = "任务提醒"
        val content = buildTaskContent(task)
        
        val intent = NotificationNavigationHandler(context)
            .createNavigationIntent(NotificationType.TASK_REMINDER, task.id)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val priority = when (task.priority) {
            takagi.ru.saison.domain.model.Priority.URGENT -> NotificationPriority.MAX
            takagi.ru.saison.domain.model.Priority.HIGH -> NotificationPriority.HIGH
            takagi.ru.saison.domain.model.Priority.MEDIUM -> NotificationPriority.DEFAULT
            takagi.ru.saison.domain.model.Priority.LOW -> NotificationPriority.LOW
        }
        
        return NotificationCompat.Builder(context, NotificationChannels.TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(priority.value)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(buildCompleteAction(task.id))
            .addAction(buildSnoozeAction(task.id))
            .build()
    }
    
    /**
     * 构建课程提醒通知
     * 
     * @param course 课程对象
     * @param startTime 上课时间
     * @return 通知对象
     */
    fun buildCourseReminder(course: Course, startTime: LocalDateTime): Notification {
        val title = "课程提醒"
        val content = buildCourseContent(course, startTime)
        
        val intent = NotificationNavigationHandler(context)
            .createNavigationIntent(NotificationType.COURSE_REMINDER, course.id)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            course.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, NotificationChannels.COURSE_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationPriority.HIGH.value)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(buildViewDetailsAction(course.id, NotificationType.COURSE_REMINDER))
            .build()
    }
    
    /**
     * 构建番茄钟提醒通知
     * 
     * @param sessionId 会话ID
     * @param isWorkEnd 是否为工作时段结束
     * @return 通知对象
     */
    fun buildPomodoroReminder(sessionId: Long, isWorkEnd: Boolean): Notification {
        val title = if (isWorkEnd) "工作时段结束" else "休息时段结束"
        val content = if (isWorkEnd) "该休息一下了！" else "休息结束，继续加油！"
        
        val intent = NotificationNavigationHandler(context).createNavigationIntent(
            if (isWorkEnd) NotificationType.POMODORO_WORK_END else NotificationType.POMODORO_BREAK_END,
            sessionId
        )
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, NotificationChannels.POMODORO_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationPriority.MAX.value)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true) // 锁屏显示
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
    
    /**
     * 构建任务摘要通知
     * 
     * @param tasks 任务列表
     * @return 通知对象
     */
    fun buildSummaryNotification(tasks: List<Task>): Notification {
        val title = "任务提醒"
        val content = "您有 ${tasks.size} 个任务即将到期"
        
        val intent = NotificationNavigationHandler(context)
            .createNavigationIntent(NotificationType.TASK_SUMMARY, 0L)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(content)
        
        tasks.take(5).forEach { task ->
            inboxStyle.addLine("• ${task.title}")
        }
        
        if (tasks.size > 5) {
            inboxStyle.addLine("还有 ${tasks.size - 5} 个任务...")
        }
        
        return NotificationCompat.Builder(context, NotificationChannels.TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inboxStyle)
            .setPriority(NotificationPriority.HIGH.value)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("task_reminders")
            .setGroupSummary(true)
            .build()
    }
    
    /**
     * 构建任务内容文本
     */
    private fun buildTaskContent(task: Task): String {
        val parts = mutableListOf<String>()
        
        // 任务标题
        parts.add(task.title)
        
        // 截止时间
        task.dueDate?.let {
            parts.add("截止时间: ${it.format(dateTimeFormatter)}")
        }
        
        // 优先级
        val priorityText = when (task.priority) {
            takagi.ru.saison.domain.model.Priority.URGENT -> "紧急"
            takagi.ru.saison.domain.model.Priority.HIGH -> "高优先级"
            takagi.ru.saison.domain.model.Priority.MEDIUM -> "中优先级"
            takagi.ru.saison.domain.model.Priority.LOW -> "低优先级"
        }
        parts.add(priorityText)
        
        // 标签
        if (task.tags.isNotEmpty()) {
            parts.add("标签: ${task.tags.first().name}")
        }
        
        return parts.joinToString(" | ")
    }
    
    /**
     * 构建课程内容文本
     */
    private fun buildCourseContent(course: Course, startTime: LocalDateTime): String {
        val parts = mutableListOf<String>()
        
        // 课程名称
        parts.add(course.name)
        
        // 上课时间
        parts.add("时间: ${startTime.format(timeFormatter)}")
        
        // 地点
        course.location?.let {
            parts.add("地点: $it")
        }
        
        // 教师
        course.instructor?.let {
            parts.add("教师: $it")
        }
        
        return parts.joinToString(" | ")
    }
    
    /**
     * 构建"标记为完成"操作
     */
    private fun buildCompleteAction(taskId: Long): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            0,
            "完成",
            pendingIntent
        ).build()
    }
    
    /**
     * 构建"稍后提醒"操作
     */
    private fun buildSnoozeAction(taskId: Long): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 10000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            0,
            "稍后提醒",
            pendingIntent
        ).build()
    }
    
    /**
     * 构建"查看详情"操作
     */
    private fun buildViewDetailsAction(itemId: Long, type: NotificationType): NotificationCompat.Action {
        val intent = NotificationNavigationHandler(context).createNavigationIntent(type, itemId)

        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt() + 20000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            0,
            "查看详情",
            pendingIntent
        ).build()
    }
}
