package takagi.ru.saison.data.repository

import android.content.Context
import takagi.ru.saison.ui.widget.model.WidgetData
import java.time.LocalDate

/**
 * 课程小组件数据仓库接口
 */
interface CourseWidgetRepository {
    /**
     * 获取小组件显示所需的完整数据
     * @param today 今天的日期
     * @return 小组件数据
     */
    suspend fun getWidgetData(today: LocalDate = LocalDate.now()): WidgetData
    
    /**
     * 更新所有课程小组件
     * @param context Android上下文
     */
    suspend fun updateWidget(context: Context)
}
