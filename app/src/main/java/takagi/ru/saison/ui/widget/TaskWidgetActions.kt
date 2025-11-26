package takagi.ru.saison.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

/**
 * 任务小组件交互动作
 * 处理完成状态切换和星标切换
 */

// Action Parameters Keys
object TaskWidgetActionKeys {
    val TASK_ID_KEY = ActionParameters.Key<Long>("taskId")
}

/**
 * 切换任务完成状态 - 优化版
 * 
 * 改进点：
 * 1. 使用 forceUpdateWidget() 确保立即视觉反馈
 * 2. 详细的性能日志记录
 * 3. 完善的错误处理
 * 4. 目标响应时间 < 100ms
 */
class ToggleCompleteAction : ActionCallback {
    
    companion object {
        private const val TAG = "ToggleCompleteAction"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d(TAG, "=== START: Toggle completion ===")
        
        try {
            // 步骤1: 验证参数
            val taskId = parameters[TaskWidgetActionKeys.TASK_ID_KEY]
            if (taskId == null) {
                android.util.Log.e(TAG, "FAILED: Task ID parameter is null")
                return
            }
            android.util.Log.d(TAG, "Task ID: $taskId")
            
            // 步骤2: 获取Application Context
            val appContext = context.applicationContext
            android.util.Log.d(TAG, "Application context: ${appContext.javaClass.simpleName}")
            
            // 步骤3: 验证Application类型
            val app = appContext as? takagi.ru.saison.SaisonApplication
            if (app == null) {
                android.util.Log.e(TAG, "FAILED: Application context is not SaisonApplication")
                return
            }
            android.util.Log.d(TAG, "SaisonApplication verified")
            
            // 步骤4: 获取EntryPoint
            val entryPoint = try {
                EntryPointAccessors.fromApplication(
                    app,
                    WidgetEntryPoints.TaskWidget::class.java
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Cannot get EntryPoint", e)
                return
            }
            android.util.Log.d(TAG, "EntryPoint obtained")
            
            // 步骤5: 获取TaskRepository
            val taskRepository = try {
                entryPoint.taskRepository()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Cannot get TaskRepository", e)
                return
            }
            android.util.Log.d(TAG, "TaskRepository obtained")
            
            // 步骤6: 获取任务
            val task = try {
                taskRepository.getTaskById(taskId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Error getting task $taskId", e)
                return
            }
            
            if (task == null) {
                android.util.Log.e(TAG, "FAILED: Task $taskId not found in database")
                return
            }
            android.util.Log.d(TAG, "Task found: '${task.title}', current isCompleted=${task.isCompleted}")
            
            // 步骤7: 切换完成状态
            val newCompletionStatus = !task.isCompleted
            val completedAt = if (newCompletionStatus) {
                java.time.LocalDateTime.now()
            } else {
                null
            }
            android.util.Log.d(TAG, "New completion status: $newCompletionStatus")
            
            // 步骤8: 更新数据库
            val dbUpdateStart = System.currentTimeMillis()
            val updatedTask = task.copy(
                isCompleted = newCompletionStatus,
                completedAt = completedAt
            )
            
            try {
                taskRepository.updateTask(updatedTask)
                val dbUpdateDuration = System.currentTimeMillis() - dbUpdateStart
                android.util.Log.d(TAG, "Database updated successfully in ${dbUpdateDuration}ms")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Error updating database", e)
                return
            }
            
            // 步骤9: 强制更新小组件 - 确保立即视觉反馈
            val widgetUpdateStart = System.currentTimeMillis()
            try {
                val widgetRepository = entryPoint.taskWidgetRepository()
                widgetRepository.forceUpdateWidget(appContext)
                val widgetUpdateDuration = System.currentTimeMillis() - widgetUpdateStart
                android.util.Log.d(TAG, "Widget force update completed in ${widgetUpdateDuration}ms")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Widget update failed", e)
                // 不阻止操作完成，数据库已更新
            }
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d(TAG, "=== SUCCESS: Toggle completion completed in ${duration}ms ===")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.e(TAG, "=== FAILED: Unexpected error after ${duration}ms ===", e)
            e.printStackTrace()
        }
    }
}

/**
 * 切换任务星标状态 - 优化版
 * 
 * 改进点：
 * 1. 使用 forceUpdateWidget() 确保立即视觉反馈
 * 2. 详细的性能日志记录
 * 3. 完善的错误处理
 * 4. 目标响应时间 < 100ms
 */
class ToggleStarAction : ActionCallback {
    
    companion object {
        private const val TAG = "ToggleStarAction"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d(TAG, "=== START: Toggle star ===")
        
        try {
            // 步骤1: 验证参数
            val taskId = parameters[TaskWidgetActionKeys.TASK_ID_KEY]
            if (taskId == null) {
                android.util.Log.e(TAG, "FAILED: Task ID parameter is null")
                return
            }
            android.util.Log.d(TAG, "Task ID: $taskId")
            
            // 步骤2: 获取Application Context
            val appContext = context.applicationContext
            android.util.Log.d(TAG, "Application context: ${appContext.javaClass.simpleName}")
            
            // 步骤3: 验证Application类型
            val app = appContext as? takagi.ru.saison.SaisonApplication
            if (app == null) {
                android.util.Log.e(TAG, "FAILED: Application context is not SaisonApplication")
                return
            }
            android.util.Log.d(TAG, "SaisonApplication verified")
            
            // 步骤4: 获取EntryPoint
            val entryPoint = try {
                EntryPointAccessors.fromApplication(
                    app,
                    WidgetEntryPoints.TaskWidget::class.java
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Cannot get EntryPoint", e)
                return
            }
            android.util.Log.d(TAG, "EntryPoint obtained")
            
            // 步骤5: 获取TaskRepository
            val taskRepository = try {
                entryPoint.taskRepository()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Cannot get TaskRepository", e)
                return
            }
            android.util.Log.d(TAG, "TaskRepository obtained")
            
            // 步骤6: 获取任务
            val task = try {
                taskRepository.getTaskById(taskId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Error getting task $taskId", e)
                return
            }
            
            if (task == null) {
                android.util.Log.e(TAG, "FAILED: Task $taskId not found in database")
                return
            }
            android.util.Log.d(TAG, "Task found: '${task.title}', current isFavorite=${task.isFavorite}")
            
            // 步骤7: 切换星标状态
            val newStarStatus = !task.isFavorite
            android.util.Log.d(TAG, "New star status: $newStarStatus")
            
            // 步骤8: 更新数据库
            val dbUpdateStart = System.currentTimeMillis()
            val updatedTask = task.copy(isFavorite = newStarStatus)
            
            try {
                taskRepository.updateTask(updatedTask)
                val dbUpdateDuration = System.currentTimeMillis() - dbUpdateStart
                android.util.Log.d(TAG, "Database updated successfully in ${dbUpdateDuration}ms")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "FAILED: Error updating database", e)
                return
            }
            
            // 步骤9: 强制更新小组件 - 确保立即视觉反馈
            val widgetUpdateStart = System.currentTimeMillis()
            try {
                val widgetRepository = entryPoint.taskWidgetRepository()
                widgetRepository.forceUpdateWidget(appContext)
                val widgetUpdateDuration = System.currentTimeMillis() - widgetUpdateStart
                android.util.Log.d(TAG, "Widget force update completed in ${widgetUpdateDuration}ms")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Widget update failed", e)
                // 不阻止操作完成，数据库已更新
            }
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d(TAG, "=== SUCCESS: Toggle star completed in ${duration}ms ===")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.e(TAG, "=== FAILED: Unexpected error after ${duration}ms ===", e)
            e.printStackTrace()
        }
    }
}

/**
 * 打开应用到任务列表页面
 * 
 * 注意：由于Widget的限制，无法直接触发应用内的bottom sheet
 * 用户打开应用后可以点击FAB按钮来创建新任务
 */
class OpenCreateTaskAction : ActionCallback {
    
    companion object {
        private const val TAG = "OpenCreateTaskAction"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        android.util.Log.d(TAG, "=== START: Open app ===")
        
        try {
            // 创建Intent打开MainActivity
            val intent = android.content.Intent(context, takagi.ru.saison.MainActivity::class.java).apply {
                // FLAG_ACTIVITY_NEW_TASK: 在新任务中启动Activity
                // FLAG_ACTIVITY_CLEAR_TOP: 如果Activity已存在，清除其上的所有Activity并复用
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                       android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            android.util.Log.d(TAG, "Intent created with flags: NEW_TASK | CLEAR_TOP")
            
            // 启动Activity
            context.startActivity(intent)
            android.util.Log.d(TAG, "=== SUCCESS: App launched ===")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "=== FAILED: Cannot launch app ===", e)
            e.printStackTrace()
        }
    }
}

/**
 * 打开任务详情页面
 * 
 * 通过Intent extras传递taskId，在MainActivity中处理导航
 */
class OpenTaskDetailAction : ActionCallback {
    
    companion object {
        private const val TAG = "OpenTaskDetailAction"
        const val EXTRA_TASK_ID = "widget_task_id"
        const val EXTRA_NAVIGATE_TO = "widget_navigate_to"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        android.util.Log.d(TAG, "=== START: Open task detail ===")
        
        try {
            val taskId = parameters[TaskWidgetActionKeys.TASK_ID_KEY]
            if (taskId == null) {
                android.util.Log.e(TAG, "FAILED: Task ID parameter is null")
                return
            }
            android.util.Log.d(TAG, "Task ID: $taskId")
            
            // 创建Intent打开MainActivity，并传递taskId
            val intent = android.content.Intent(context, takagi.ru.saison.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or 
                       android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_NAVIGATE_TO, "task_preview")
            }
            
            android.util.Log.d(TAG, "Intent created with taskId: $taskId")
            
            // 启动Activity
            context.startActivity(intent)
            android.util.Log.d(TAG, "=== SUCCESS: App launched with task detail ===")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "=== FAILED: Cannot launch app ===", e)
            e.printStackTrace()
        }
    }
}
