package takagi.ru.saison.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 任务小组件接收器
 */
class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    
    override val glanceAppWidget: GlanceAppWidget
        get() = TaskWidget()
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        android.util.Log.d("TaskWidgetReceiver", "Widget updated for ${appWidgetIds.size} instances")
        
        // 如果有小组件实例，启动定期更新
        if (appWidgetIds.isNotEmpty()) {
            TaskWidgetUpdateScheduler.schedule(context)
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        
        // 检查是否还有其他小组件实例
        val remainingWidgets = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(android.content.ComponentName(context, TaskWidgetReceiver::class.java))
        
        // 如果没有小组件了，取消定期更新
        if (remainingWidgets.isEmpty()) {
            TaskWidgetUpdateScheduler.cancel(context)
            android.util.Log.d("TaskWidgetReceiver", "All widgets removed, cancelled updates")
        }
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个小组件被移除时取消定期更新
        TaskWidgetUpdateScheduler.cancel(context)
        android.util.Log.d("TaskWidgetReceiver", "Widget disabled, cancelled updates")
    }
}
