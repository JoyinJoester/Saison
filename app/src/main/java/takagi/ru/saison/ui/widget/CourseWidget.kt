package takagi.ru.saison.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.data.repository.CourseWidgetRepository
import takagi.ru.saison.ui.widget.model.WidgetData
import javax.inject.Inject

/**
 * 课程表小组件主类
 */
class CourseWidget @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CourseWidgetRepository
) : GlanceAppWidget() {

    companion object {
        private const val TAG = "CourseWidget"
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "provideGlance called for widget $id")
        val startTime = System.currentTimeMillis()
        
        // 获取小组件数据
        val widgetData = try {
            val data = repository.getWidgetData()
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Widget data fetched in ${duration}ms: hasActiveSemester=${data.hasActiveSemester}, today=${data.todayCourses.size} courses, tomorrow=${data.tomorrowCourses.size} courses")
            data
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Failed to fetch widget data after ${duration}ms", e)
            WidgetData.empty()
        }

        provideContent {
            CourseWidgetContent(widgetData)
        }
    }
}
