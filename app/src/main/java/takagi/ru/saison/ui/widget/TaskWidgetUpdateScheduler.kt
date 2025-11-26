package takagi.ru.saison.ui.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 任务小组件更新调度器
 * 负责设置定期更新任务
 */
object TaskWidgetUpdateScheduler {
    
    /**
     * 调度定期更新任务
     * 每30分钟更新一次小组件
     */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<TaskWidgetWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TaskWidgetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        android.util.Log.d("TaskWidgetScheduler", "Scheduled periodic widget updates")
    }
    
    /**
     * 取消定期更新任务
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TaskWidgetWorker.WORK_NAME)
        android.util.Log.d("TaskWidgetScheduler", "Cancelled periodic widget updates")
    }
    
    /**
     * 立即触发一次更新
     */
    fun triggerUpdate(context: Context) {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<TaskWidgetWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        android.util.Log.d("TaskWidgetScheduler", "Triggered immediate widget update")
    }
}
