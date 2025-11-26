package takagi.ru.saison.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 课程表小组件接收器
 */
@AndroidEntryPoint
class CourseWidgetReceiver : GlanceAppWidgetReceiver() {

    @Inject
    lateinit var courseWidget: CourseWidget

    override val glanceAppWidget: GlanceAppWidget
        get() = courseWidget
}
