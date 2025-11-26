package takagi.ru.saison.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import takagi.ru.saison.ui.widget.model.TaskWidgetData

/**
 * 任务小组件主类 - 重构版
 * 
 * 注意：由于GlanceAppWidget的限制，我们不能直接在这里使用依赖注入。
 * 数据通过Hilt EntryPoint获取。
 * 
 * 改进点：
 * 1. 详细的日志记录
 * 2. 完善的错误处理
 * 3. 性能监控
 * 4. 确保使用applicationContext
 */
class TaskWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "TaskWidget"
        private const val DATA_FETCH_TIMEOUT_MS = 5000L  // 5秒超时
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "=== START: provideGlance for widget $id ===")
        
        // 获取小组件数据
        val widgetData = getWidgetDataSafely(context, startTime)
        
        // 渲染内容
        provideContent {
            TaskWidgetContent(widgetData)
        }
        
        val totalDuration = System.currentTimeMillis() - startTime
        Log.d(TAG, "=== END: provideGlance completed in ${totalDuration}ms ===")
    }
    
    /**
     * 安全地获取小组件数据
     * 包含完整的错误处理和日志记录
     */
    private suspend fun getWidgetDataSafely(context: Context, startTime: Long): TaskWidgetData {
        try {
            // 步骤1: 获取Application Context
            val appContext = context.applicationContext
            Log.d(TAG, "Application context: ${appContext.javaClass.simpleName}")
            
            // 步骤2: 验证Application类型
            val app = appContext as? takagi.ru.saison.SaisonApplication
            if (app == null) {
                Log.e(TAG, "FAILED: Application context is not SaisonApplication")
                return TaskWidgetData.empty()
            }
            Log.d(TAG, "SaisonApplication verified")
            
            // 步骤3: 获取EntryPoint
            val entryPoint = try {
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    app,
                    WidgetEntryPoints.TaskWidget::class.java
                )
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Cannot get EntryPoint", e)
                return TaskWidgetData.empty()
            }
            Log.d(TAG, "EntryPoint obtained")
            
            // 步骤4: 获取TaskWidgetRepository
            val repository = try {
                entryPoint.taskWidgetRepository()
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Cannot get TaskWidgetRepository", e)
                return TaskWidgetData.empty()
            }
            Log.d(TAG, "TaskWidgetRepository obtained: ${repository.javaClass.simpleName}")
            
            // 步骤5: 获取数据
            val data = try {
                repository.getWidgetData()
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Error fetching widget data", e)
                return TaskWidgetData.empty()
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "SUCCESS: Data fetched in ${duration}ms")
            Log.d(TAG, "  - Total tasks: ${data.allTasks.size}")
            Log.d(TAG, "  - Incomplete: ${data.incompleteCount}")
            Log.d(TAG, "  - Completed today: ${data.completedTodayCount}")
            
            return data
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "FAILED: Unexpected error after ${duration}ms", e)
            e.printStackTrace()
            return TaskWidgetData.empty()
        }
    }
}

// TaskWidgetEntryPoint moved to WidgetEntryPoints.kt for centralized management
