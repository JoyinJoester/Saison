package takagi.ru.saison.data.repository

import android.content.Context
import takagi.ru.saison.ui.widget.model.TaskWidgetData

/**
 * 任务小组件数据仓库接口
 */
interface TaskWidgetRepository {
    /**
     * 获取小组件数据
     */
    suspend fun getWidgetData(): TaskWidgetData
    
    /**
     * 更新小组件（标准更新，可能被系统节流）
     */
    suspend fun updateWidget(context: Context)
    
    /**
     * 强制更新小组件（用于用户交互，绕过系统节流）
     * 
     * 此方法专为用户触发的操作设计，确保立即视觉反馈：
     * - 使用 Dispatchers.Default 以获得更快响应
     * - 实施多层更新策略以绕过 Android 节流
     * - 提供详细的性能日志
     * 
     * @param context Android context
     */
    suspend fun forceUpdateWidget(context: Context)
}
