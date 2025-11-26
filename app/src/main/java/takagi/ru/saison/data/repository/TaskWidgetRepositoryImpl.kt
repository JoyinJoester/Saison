package takagi.ru.saison.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import takagi.ru.saison.ui.widget.TaskWidget
import takagi.ru.saison.ui.widget.model.TaskWidgetData
import takagi.ru.saison.ui.widget.model.toWidgetTask
import javax.inject.Inject

/**
 * 任务小组件数据仓库实现 - 重构版
 * 
 * 改进点：
 * 1. 使用Dispatchers.IO进行数据库操作
 * 2. 使用Dispatchers.Main进行UI更新
 * 3. 详细的日志记录
 * 4. 完善的错误处理
 * 5. 性能监控
 */
class TaskWidgetRepositoryImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : TaskWidgetRepository {
    
    companion object {
        private const val TAG = "TaskWidgetRepository"
    }
    
    override suspend fun getWidgetData(): TaskWidgetData = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "=== START: Fetching widget data ===")
        
        try {
            // 步骤1: 获取所有任务
            val allTasks = try {
                taskRepository.getAllTasks().first()
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Error getting tasks from repository", e)
                return@withContext TaskWidgetData.empty()
            }
            Log.d(TAG, "Retrieved ${allTasks.size} tasks from database")
            
            // 步骤2: 转换为WidgetTask
            val widgetTasks = try {
                allTasks.map { it.toWidgetTask() }
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Error converting tasks to WidgetTask", e)
                return@withContext TaskWidgetData.empty()
            }
            Log.d(TAG, "Converted ${widgetTasks.size} tasks to WidgetTask")
            
            // 步骤3: 计算统计数据
            val incompleteCount = widgetTasks.count { !it.isCompleted }
            val completedTodayCount = allTasks.count { task ->
                task.isCompleted && task.completedAt?.toLocalDate() == java.time.LocalDate.now()
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "=== SUCCESS: Data fetched in ${duration}ms ===")
            Log.d(TAG, "  - Total: ${widgetTasks.size}")
            Log.d(TAG, "  - Incomplete: $incompleteCount")
            Log.d(TAG, "  - Completed today: $completedTodayCount")
            
            TaskWidgetData(
                allTasks = widgetTasks,
                incompleteCount = incompleteCount,
                completedTodayCount = completedTodayCount
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "=== FAILED: Unexpected error after ${duration}ms ===", e)
            e.printStackTrace()
            TaskWidgetData.empty()
        }
    }
    
    override suspend fun updateWidget(context: Context) = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "=== START: Updating widget ===")
        
        try {
            // 步骤1: 确保使用Application Context
            val appContext = context.applicationContext
            Log.d(TAG, "Using application context: ${appContext.javaClass.simpleName}")
            
            // 步骤2: 获取GlanceAppWidgetManager
            val glanceManager = try {
                GlanceAppWidgetManager(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Cannot create GlanceAppWidgetManager", e)
                return@withContext
            }
            
            // 步骤3: 获取所有widget实例
            val glanceIds = try {
                glanceManager.getGlanceIds(TaskWidget::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Cannot get widget instances", e)
                return@withContext
            }
            
            Log.d(TAG, "Found ${glanceIds.size} widget instances")
            
            if (glanceIds.isEmpty()) {
                Log.w(TAG, "No widget instances found, skipping update")
                return@withContext
            }
            
            // 步骤4: 更新所有实例
            try {
                TaskWidget().updateAll(appContext)
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "=== SUCCESS: All widgets updated in ${duration}ms ===")
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Error updating widgets", e)
                e.printStackTrace()
            }
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "=== FAILED: Unexpected error after ${duration}ms ===", e)
            e.printStackTrace()
        }
    }
    
    override suspend fun forceUpdateWidget(context: Context) = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "=== START: Force updating widget (user-initiated) ===")
        Log.d(TAG, "Trigger: User interaction at ${System.currentTimeMillis()}")
        
        try {
            // 步骤1: 确保使用Application Context
            val appContext = context.applicationContext
            Log.d(TAG, "Using application context: ${appContext.javaClass.simpleName}")
            
            // 步骤2: 获取GlanceAppWidgetManager
            val glanceManager = try {
                GlanceAppWidgetManager(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Cannot create GlanceAppWidgetManager", e)
                Log.e(TAG, "Error context: ${e.message}", e)
                return@withContext
            }
            
            // 步骤3: 获取所有widget实例
            val glanceIds = try {
                glanceManager.getGlanceIds(TaskWidget::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "FAILED: Cannot get widget instances", e)
                Log.e(TAG, "Error context: ${e.message}", e)
                return@withContext
            }
            
            Log.d(TAG, "Found ${glanceIds.size} widget instances to update")
            
            if (glanceIds.isEmpty()) {
                Log.w(TAG, "No widget instances found, user may have removed widget")
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "=== END: No update needed (${duration}ms) ===")
                return@withContext
            }
            
            // 步骤4: 强制更新策略 - 三层fallback
            var updateSuccess = false
            
            // 策略1: 更新所有实例 + 逐个更新确保生效
            try {
                val strategy1Start = System.currentTimeMillis()
                Log.d(TAG, "Attempting Strategy 1: UpdateAll + Individual updates")
                
                // 先调用 updateAll
                TaskWidget().updateAll(appContext)
                
                // 再逐个更新每个实例以确保生效
                glanceIds.forEach { glanceId ->
                    try {
                        TaskWidget().update(appContext, glanceId)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update instance $glanceId", e)
                    }
                }
                
                val strategy1Duration = System.currentTimeMillis() - strategy1Start
                Log.d(TAG, "✓ Strategy 1 SUCCESS: Updated ${glanceIds.size} instances in ${strategy1Duration}ms")
                updateSuccess = true
            } catch (e: Exception) {
                Log.w(TAG, "✗ Strategy 1 FAILED: ${e.message}", e)
            }
            
            // 策略2: 仅逐个更新（如果策略1失败）
            if (!updateSuccess) {
                try {
                    val strategy2Start = System.currentTimeMillis()
                    Log.d(TAG, "Attempting Strategy 2: Individual updates only")
                    
                    var successCount = 0
                    glanceIds.forEach { glanceId ->
                        try {
                            TaskWidget().update(appContext, glanceId)
                            successCount++
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to update instance $glanceId", e)
                        }
                    }
                    
                    val strategy2Duration = System.currentTimeMillis() - strategy2Start
                    if (successCount > 0) {
                        Log.d(TAG, "✓ Strategy 2 SUCCESS: Updated $successCount/${glanceIds.size} instances in ${strategy2Duration}ms")
                        updateSuccess = true
                    } else {
                        Log.e(TAG, "✗ Strategy 2 FAILED: No instances updated")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Strategy 2 FAILED: ${e.message}", e)
                }
            }
            
            // 策略3: 使用 AppWidgetManager 强制刷新（最后手段）
            if (!updateSuccess) {
                try {
                    val strategy3Start = System.currentTimeMillis()
                    Log.d(TAG, "Attempting Strategy 3: AppWidgetManager force refresh")
                    
                    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(appContext)
                    val componentName = android.content.ComponentName(appContext, TaskWidget::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    
                    if (appWidgetIds.isNotEmpty()) {
                        // 通知数据变化
                        appWidgetIds.forEach { appWidgetId ->
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list)
                        }
                        
                        // 再次尝试更新
                        TaskWidget().updateAll(appContext)
                        
                        val strategy3Duration = System.currentTimeMillis() - strategy3Start
                        Log.d(TAG, "✓ Strategy 3 SUCCESS: Force refreshed ${appWidgetIds.size} instances in ${strategy3Duration}ms")
                        updateSuccess = true
                    } else {
                        Log.e(TAG, "✗ Strategy 3 FAILED: No AppWidget IDs found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Strategy 3 FAILED: ${e.message}", e)
                    Log.e(TAG, "Full error context:", e)
                }
            }
            
            val totalDuration = System.currentTimeMillis() - startTime
            
            if (updateSuccess) {
                Log.d(TAG, "=== SUCCESS: Force update completed in ${totalDuration}ms ===")
                Log.d(TAG, "Performance: Total=${totalDuration}ms, Instances=${glanceIds.size}")
            } else {
                Log.e(TAG, "=== CRITICAL: All update strategies failed after ${totalDuration}ms ===")
                Log.e(TAG, "Diagnostic info: Instances=${glanceIds.size}, Context=${appContext.javaClass.simpleName}")
            }
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "=== FAILED: Unexpected error in force update after ${duration}ms ===", e)
            Log.e(TAG, "Error details: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            e.printStackTrace()
        }
    }
}
