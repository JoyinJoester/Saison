package takagi.ru.saison.ui.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import takagi.ru.saison.ui.widget.model.TaskWidgetData

/**
 * 任务小组件根内容
 * 使用纯列表布局，所有尺寸都显示任务列表
 */
@Composable
fun TaskWidgetContent(data: TaskWidgetData) {
    GlanceTheme(colors = TaskWidgetTheme.colors) {
        // 使用纯列表布局，支持交互
        ListWidgetContent(data)
    }
}
