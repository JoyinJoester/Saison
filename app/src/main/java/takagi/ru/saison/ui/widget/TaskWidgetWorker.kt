package takagi.ru.saison.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 任务小组件后台更新 Worker
 * 定期更新小组件数据
 */
@HiltWorker
class TaskWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 更新所有小组件实例
            TaskWidget().updateAll(context)
            android.util.Log.d("TaskWidgetWorker", "Widget updated successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TaskWidgetWorker", "Failed to update widget", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "task_widget_update"
    }
}
