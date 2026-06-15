package takagi.ru.saison

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.DefaultSemesterInitializer
import takagi.ru.saison.ui.widget.CourseWidgetScheduler
import javax.inject.Inject

@HiltAndroidApp
class SaisonApplication : Application() {
    
    @Inject
    lateinit var widgetScheduler: CourseWidgetScheduler
    
    @Inject
    lateinit var defaultSemesterInitializer: DefaultSemesterInitializer
    
    @Inject
    lateinit var notificationManager: takagi.ru.saison.notification.SaisonNotificationManager
    
    @Inject
    lateinit var taskRepository: takagi.ru.saison.data.repository.TaskRepository
    
    @Inject
    lateinit var courseRepository: takagi.ru.saison.data.repository.CourseRepository
    
    @Inject
    lateinit var preferencesManager: takagi.ru.saison.data.local.datastore.PreferencesManager
    
    @Inject
    lateinit var quickInputManager: takagi.ru.saison.notification.QuickInputNotificationManager
    
    @Inject
    lateinit var permissionManager: takagi.ru.saison.notification.NotificationPermissionManager
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "SaisonApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化通知系统
        try {
            Log.d(TAG, "Initializing notification system")
            notificationManager.initialize()
            Log.d(TAG, "Notification system initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize notification system", e)
        }
        
        // 在后台线程确保默认学期存在
        // Requirements: 2.1, 2.2, 2.3, 2.4
        applicationScope.launch {
            try {
                Log.d(TAG, "Initializing default semester")
                defaultSemesterInitializer.ensureDefaultSemester()
                Log.d(TAG, "Default semester initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize default semester", e)
                // 不阻塞应用启动，错误已在初始化器中处理
            }
        }
        
        // 启动小组件定期更新
        widgetScheduler.schedulePeriodicUpdate()
        
        // 监听通知设置变化
        setupNotificationSettingsObservers()
        
        // 恢复快捷输入通知
        restoreQuickInputNotification()
    }
    
    /**
     * 恢复快捷输入通知
     */
    private fun restoreQuickInputNotification() {
        applicationScope.launch {
            // 同时监听快捷输入开关和 Plus 状态
            kotlinx.coroutines.flow.combine(
                preferencesManager.quickInputEnabled,
                preferencesManager.isPlusActivated
            ) { quickInputEnabled, isPlusActivated ->
                quickInputEnabled && isPlusActivated
            }.collect { shouldShow ->
                if (shouldShow && permissionManager.checkNotificationPermission()) {
                    Log.d(TAG, "Restoring quick input notification")
                    quickInputManager.showQuickInputNotification()
                } else {
                    Log.d(TAG, "Quick input disabled, Plus not activated, or permission not granted")
                    quickInputManager.dismissQuickInputNotification()
                }
            }
        }
    }
    
    /**
     * 设置通知设置监听器
     */
    private fun setupNotificationSettingsObservers() {
        // 监听任务提醒开关
        applicationScope.launch {
            var previousTaskRemindersEnabled: Boolean? = null
            preferencesManager.taskRemindersEnabled.collect { enabled ->
                if (previousTaskRemindersEnabled != null && previousTaskRemindersEnabled != enabled) {
                    if (enabled) {
                        // 启用时重新调度所有任务提醒
                        Log.d(TAG, "Task reminders enabled, rescheduling all task notifications")
                        rescheduleAllTaskReminders()
                    } else {
                        // 禁用时取消所有任务提醒
                        Log.d(TAG, "Task reminders disabled, cancelling all task notifications")
                        notificationManager.cancelAllTaskReminders()
                    }
                }
                previousTaskRemindersEnabled = enabled
            }
        }
        
        // 监听课程提醒开关
        applicationScope.launch {
            var previousCourseRemindersEnabled: Boolean? = null
            preferencesManager.courseRemindersEnabled.collect { enabled ->
                if (previousCourseRemindersEnabled != null && previousCourseRemindersEnabled != enabled) {
                    if (enabled) {
                        // 启用时重新调度所有课程提醒
                        Log.d(TAG, "Course reminders enabled, rescheduling all course notifications")
                        rescheduleAllCourseReminders()
                    } else {
                        // 禁用时取消所有课程提醒
                        Log.d(TAG, "Course reminders disabled, cancelling all course notifications")
                        notificationManager.cancelAllCourseReminders()
                    }
                }
                previousCourseRemindersEnabled = enabled
            }
        }
    }
    
    /**
     * 重新调度所有任务提醒
     */
    private suspend fun rescheduleAllTaskReminders() {
        taskRepository.getAllTasks().first()
            .filter { !it.isCompleted && (it.dueDate != null || it.reminderTime != null) }
            .forEach { task ->
                notificationManager.scheduleTaskReminder(task)
            }
    }
    
    /**
     * 重新调度所有课程提醒
     */
    private suspend fun rescheduleAllCourseReminders() {
        courseRepository.getAllCourses().first().forEach { course ->
            notificationManager.scheduleCourseReminder(course)
        }
    }
}
