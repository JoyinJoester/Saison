package takagi.ru.saison.ui.widget

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.worker.CourseWidgetWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程表小组件调度器
 * 负责配置和管理小组件的定期更新
 */
@Singleton
class CourseWidgetScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 启动定期更新任务
     */
    fun schedulePeriodicUpdate() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<CourseWidgetWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CourseWidgetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * 立即更新小组件
     */
    fun updateNow() {
        val updateWorkRequest = OneTimeWorkRequestBuilder<CourseWidgetWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(updateWorkRequest)
    }

    /**
     * 取消定期更新任务
     */
    fun cancelPeriodicUpdate() {
        WorkManager.getInstance(context).cancelUniqueWork(CourseWidgetWorker.WORK_NAME)
    }
}
