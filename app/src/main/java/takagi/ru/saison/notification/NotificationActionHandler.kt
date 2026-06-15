package takagi.ru.saison.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.data.repository.TaskRepository
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知操作处理器
 * 处理通知中的用户操作
 */
@Singleton
class NotificationActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val notificationManager: SaisonNotificationManager
) {
    
    /**
     * 处理"标记为完成"操作
     * 
     * @param taskId 任务ID
     */
    suspend fun handleMarkAsComplete(taskId: Long) {
        try {
            // 标记任务为已完成
            taskRepository.toggleTaskCompletion(taskId, true)
            android.util.Log.d("NotificationActionHandler", "Task $taskId marked as complete")
        } catch (e: Exception) {
            android.util.Log.e("NotificationActionHandler", "Failed to mark task as complete", e)
        }
    }
    
    /**
     * 处理"稍后提醒"操作
     * 
     * @param taskId 任务ID
     * @param delayMinutes 延迟分钟数（默认30分钟）
     */
    suspend fun handleSnooze(taskId: Long, delayMinutes: Int = 30) {
        try {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                // 取消当前提醒
                notificationManager.cancelTaskReminder(taskId)
                
                val newReminderTime = LocalDateTime.now().plusMinutes(delayMinutes.toLong())
                val updatedTask = task.copy(
                    reminderTime = newReminderTime,
                    updatedAt = LocalDateTime.now()
                )
                taskRepository.updateTask(updatedTask)
                android.util.Log.d("NotificationActionHandler", "Task $taskId snoozed for $delayMinutes minutes")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationActionHandler", "Failed to snooze task", e)
        }
    }
    
    /**
     * 处理"查看详情"操作
     * 
     * @param itemId 项目ID
     * @param type 通知类型
     */
    suspend fun handleViewDetails(itemId: Long, type: takagi.ru.saison.domain.model.notification.NotificationType) {
        // 这个方法主要用于记录日志，实际导航由 MainActivity 处理
        android.util.Log.d("NotificationActionHandler", "View details for $type item $itemId")
    }
}
