package takagi.ru.saison.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.notification.NotificationChannels
import takagi.ru.saison.domain.model.notification.NotificationData
import takagi.ru.saison.domain.model.notification.NotificationPriority
import takagi.ru.saison.domain.model.notification.NotificationType
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationScheduler"
private const val KEY_NOTIFICATION_ID = "notification_id"
private const val KEY_NOTIFICATION_TYPE = "notification_type"
private const val KEY_ITEM_ID = "item_id"
private const val KEY_TITLE = "title"
private const val KEY_CONTENT = "content"
private const val KEY_PRIORITY = "priority"

/**
 * 通知调度器
 * 负责使用 WorkManager 和 AlarmManager 调度通知
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * 调度单次通知
     * 
     * @param notificationId 通知ID
     * @param triggerTime 触发时间（毫秒时间戳）
     * @param data 通知数据
     */
    fun scheduleOneTime(
        notificationId: String,
        triggerTime: Long,
        data: NotificationData
    ) {
        val delay = triggerTime - System.currentTimeMillis()
        
        if (delay <= 0) {
            // 时间已过，不调度
            return
        }
        
        // 对于番茄钟通知，使用 AlarmManager 确保精确时间
        if (data.type == NotificationType.POMODORO_WORK_END || 
            data.type == NotificationType.POMODORO_BREAK_END) {
            scheduleWithAlarmManager(notificationId, triggerTime, data)
        } else {
            // 其他通知使用 WorkManager
            scheduleWithWorkManager(notificationId, delay, data)
        }
    }
    
    /**
     * 使用 WorkManager 调度通知
     */
    private fun scheduleWithWorkManager(
        notificationId: String,
        delay: Long,
        data: NotificationData
    ) {
        val inputData = workDataOf(
            KEY_NOTIFICATION_ID to notificationId,
            KEY_NOTIFICATION_TYPE to data.type.name,
            KEY_ITEM_ID to data.itemId,
            KEY_TITLE to data.title,
            KEY_CONTENT to data.content,
            KEY_PRIORITY to data.priority.name
        )
        
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(notificationId)
            .addTag("notification_${data.type.name}")
            .build()
        
        workManager.enqueueUniqueWork(
            notificationId,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * 使用 AlarmManager 调度精确时间通知
     */
    private fun scheduleWithAlarmManager(
        notificationId: String,
        triggerTime: Long,
        data: NotificationData
    ) {
        if (!canScheduleExactAlarms()) {
            val delay = triggerTime - System.currentTimeMillis()
            if (delay > 0) {
                Log.w(TAG, "Exact alarm unavailable, falling back to WorkManager for $notificationId")
                scheduleWithWorkManager(notificationId, delay, data)
            }
            return
        }

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            putExtra(KEY_NOTIFICATION_ID, notificationId)
            putExtra(KEY_NOTIFICATION_TYPE, data.type.name)
            putExtra(KEY_ITEM_ID, data.itemId)
            putExtra(KEY_TITLE, data.title)
            putExtra(KEY_CONTENT, data.content)
            putExtra(KEY_PRIORITY, data.priority.name)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to schedule exact alarm, falling back to WorkManager", e)
            val delay = triggerTime - System.currentTimeMillis()
            if (delay > 0) {
                scheduleWithWorkManager(notificationId, delay, data)
            }
        }
    }
    
    /**
     * 取消通知调度
     * 
     * @param notificationId 通知ID
     */
    fun cancel(notificationId: String) {
        // 取消 WorkManager 任务
        workManager.cancelUniqueWork(notificationId)
        
        // 取消 AlarmManager 闹钟
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
    
    /**
     * 取消指定类型的所有通知
     * 
     * @param type 通知类型
     */
    fun cancelAll(type: NotificationType) {
        workManager.cancelAllWorkByTag("notification_${type.name}")
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}

private data class ScheduledNotificationPayload(
    val notificationId: String,
    val type: NotificationType,
    val itemId: Long,
    val title: String,
    val content: String,
    val priority: NotificationPriority
)

private fun showScheduledNotification(
    context: Context,
    payload: ScheduledNotificationPayload
) {
    if (!context.hasNotificationPermission()) {
        Log.w(TAG, "Notification permission denied for ${payload.notificationId}")
        return
    }

    NotificationChannelManager(context).initializeChannels()

    val notification = NotificationCompat.Builder(context, payload.type.channelId())
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(payload.title)
        .setContentText(payload.content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(payload.content))
        .setPriority(payload.priority.value)
        .setContentIntent(payload.createContentPendingIntent(context))
        .setAutoCancel(true)
        .apply {
            when (payload.type) {
                NotificationType.TASK_REMINDER -> {
                    addAction(payload.createMarkCompleteAction(context))
                    addAction(payload.createSnoozeAction(context))
                }
                NotificationType.COURSE_REMINDER -> {
                    addAction(payload.createViewDetailsAction(context))
                }
                NotificationType.POMODORO_WORK_END,
                NotificationType.POMODORO_BREAK_END -> {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                }
                NotificationType.TASK_SUMMARY -> Unit
            }
        }
        .build()

    try {
        NotificationManagerCompat.from(context).notify(payload.notificationId.hashCode(), notification)
    } catch (e: SecurityException) {
        Log.w(TAG, "Unable to post notification ${payload.notificationId}", e)
    }
}

private fun ScheduledNotificationPayload.createContentPendingIntent(context: Context): PendingIntent {
    val intent = NotificationNavigationHandler(context).createNavigationIntent(type, itemId)
    return PendingIntent.getActivity(
        context,
        notificationId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun ScheduledNotificationPayload.createMarkCompleteAction(context: Context): NotificationCompat.Action {
    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_MARK_COMPLETE
        putExtra(NotificationActionReceiver.EXTRA_TASK_ID, itemId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        "${notificationId}_complete".hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Action.Builder(0, "完成", pendingIntent).build()
}

private fun ScheduledNotificationPayload.createSnoozeAction(context: Context): NotificationCompat.Action {
    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_SNOOZE
        putExtra(NotificationActionReceiver.EXTRA_TASK_ID, itemId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        "${notificationId}_snooze".hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Action.Builder(0, "稍后提醒", pendingIntent).build()
}

private fun ScheduledNotificationPayload.createViewDetailsAction(context: Context): NotificationCompat.Action {
    val pendingIntent = createContentPendingIntent(context)
    return NotificationCompat.Action.Builder(0, "查看详情", pendingIntent).build()
}

private fun NotificationType.channelId(): String = when (this) {
    NotificationType.TASK_REMINDER,
    NotificationType.TASK_SUMMARY -> NotificationChannels.TASK_REMINDERS
    NotificationType.COURSE_REMINDER -> NotificationChannels.COURSE_REMINDERS
    NotificationType.POMODORO_WORK_END,
    NotificationType.POMODORO_BREAK_END -> NotificationChannels.POMODORO_REMINDERS
}

private fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(this).areNotificationsEnabled()
    }
}

private fun notificationPayloadFromData(data: Data): ScheduledNotificationPayload? {
    val notificationId = data.getString(KEY_NOTIFICATION_ID) ?: return null
    val type = data.getString(KEY_NOTIFICATION_TYPE)
        ?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() }
        ?: return null
    val priority = data.getString(KEY_PRIORITY)
        ?.let { runCatching { NotificationPriority.valueOf(it) }.getOrNull() }
        ?: NotificationPriority.DEFAULT

    return ScheduledNotificationPayload(
        notificationId = notificationId,
        type = type,
        itemId = data.getLong(KEY_ITEM_ID, 0L),
        title = data.getString(KEY_TITLE).orEmpty(),
        content = data.getString(KEY_CONTENT).orEmpty(),
        priority = priority
    )
}

private fun notificationPayloadFromIntent(intent: Intent): ScheduledNotificationPayload? {
    val notificationId = intent.getStringExtra(KEY_NOTIFICATION_ID) ?: return null
    val type = intent.getStringExtra(KEY_NOTIFICATION_TYPE)
        ?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() }
        ?: return null
    val priority = intent.getStringExtra(KEY_PRIORITY)
        ?.let { runCatching { NotificationPriority.valueOf(it) }.getOrNull() }
        ?: NotificationPriority.DEFAULT

    return ScheduledNotificationPayload(
        notificationId = notificationId,
        type = type,
        itemId = intent.getLongExtra(KEY_ITEM_ID, 0L),
        title = intent.getStringExtra(KEY_TITLE).orEmpty(),
        content = intent.getStringExtra(KEY_CONTENT).orEmpty(),
        priority = priority
    )
}

/**
 * 通知 Worker
 * 用于在指定时间显示通知
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val payload = notificationPayloadFromData(inputData) ?: return Result.failure()
        showScheduledNotification(applicationContext, payload)
        return Result.success()
    }
}

/**
 * 通知闹钟接收器
 * 用于接收 AlarmManager 触发的通知
 */
class NotificationAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = notificationPayloadFromIntent(intent) ?: return
        showScheduledNotification(context, payload)
    }
}
