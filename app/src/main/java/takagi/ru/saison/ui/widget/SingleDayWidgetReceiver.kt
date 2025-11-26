package takagi.ru.saison.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 单日课程小组件接收器
 */
@AndroidEntryPoint
class SingleDayWidgetReceiver : GlanceAppWidgetReceiver() {

    @Inject
    lateinit var singleDayWidget: SingleDayWidget

    override val glanceAppWidget: GlanceAppWidget
        get() = singleDayWidget
}
