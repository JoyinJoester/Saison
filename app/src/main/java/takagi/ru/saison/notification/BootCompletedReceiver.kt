package takagi.ru.saison.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设备重启接收器
 * 在设备重启后重新调度所有通知
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var taskRepository: takagi.ru.saison.data.repository.TaskRepository
    
    @Inject
    lateinit var courseRepository: takagi.ru.saison.data.repository.CourseRepository
    
    @Inject
    lateinit var saisonNotificationManager: SaisonNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    rescheduleAllNotifications()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
    
    /**
     * 重新调度所有通知
     */
    private suspend fun rescheduleAllNotifications() {
        // 重新调度所有未完成任务的提醒
        taskRepository.getAllTasks().first()
            .filter { !it.isCompleted && (it.dueDate != null || it.reminderTime != null) }
            .forEach { task ->
                saisonNotificationManager.scheduleTaskReminder(task)
            }
        
        // 重新调度所有课程的提醒
        courseRepository.getAllCourses().first().forEach { course ->
            saisonNotificationManager.scheduleCourseReminder(course)
        }
    }
}
