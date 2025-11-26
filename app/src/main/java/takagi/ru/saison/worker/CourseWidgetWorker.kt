package takagi.ru.saison.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import takagi.ru.saison.ui.widget.CourseWidget

/**
 * 课程表小组件更新 Worker
 * 用于定期更新小组件数据
 */
@HiltWorker
class CourseWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val courseWidget: CourseWidget
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 更新所有小组件实例
            courseWidget.updateAll(context)
            Result.success()
        } catch (e: Exception) {
            // 如果失败,重试
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "course_widget_update"
    }
}
