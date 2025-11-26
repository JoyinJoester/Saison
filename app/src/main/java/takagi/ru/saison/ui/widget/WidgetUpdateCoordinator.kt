package takagi.ru.saison.ui.widget

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.CourseWidgetRepository
import takagi.ru.saison.data.repository.TaskWidgetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 小组件更新协调器
 * 
 * 统一管理所有小组件的更新，实现更新批处理和防抖动
 */
@Singleton
class WidgetUpdateCoordinator @Inject constructor(
    private val taskWidgetRepository: TaskWidgetRepository,
    private val courseWidgetRepository: CourseWidgetRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var taskUpdateJob: Job? = null
    private var courseUpdateJob: Job? = null
    
    companion object {
        private const val TAG = "WidgetUpdateCoordinator"
        private const val UPDATE_DEBOUNCE_MS = 1000L // 1秒防抖动窗口
    }
    
    /**
     * 任务数据变化时调用
     * 使用防抖动机制，在1秒窗口内的多次更新会被合并
     */
    fun onTaskChanged(context: Context) {
        Log.d(TAG, "Task data changed, scheduling widget update")
        
        // 使用 Application Context 避免内存泄漏
        val appContext = context.applicationContext
        
        // 取消之前的更新任务
        taskUpdateJob?.cancel()
        
        // 创建新的更新任务，延迟1秒执行
        taskUpdateJob = scope.launch {
            try {
                delay(UPDATE_DEBOUNCE_MS)
                Log.d(TAG, "Executing batched task widget update")
                val startTime = System.currentTimeMillis()
                
                taskWidgetRepository.updateWidget(appContext)
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Task widget update completed in ${duration}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update task widget", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 课程数据变化时调用
     * 使用防抖动机制，在1秒窗口内的多次更新会被合并
     */
    fun onCourseChanged(context: Context) {
        Log.d(TAG, "Course data changed, scheduling widget update")
        
        // 使用 Application Context 避免内存泄漏
        val appContext = context.applicationContext
        
        // 取消之前的更新任务
        courseUpdateJob?.cancel()
        
        // 创建新的更新任务，延迟1秒执行
        courseUpdateJob = scope.launch {
            try {
                delay(UPDATE_DEBOUNCE_MS)
                Log.d(TAG, "Executing batched course widget update")
                val startTime = System.currentTimeMillis()
                
                courseWidgetRepository.updateWidget(appContext)
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Course widget update completed in ${duration}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update course widget", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 立即更新所有小组件（不使用防抖动）
     * 用于用户主动触发的更新
     */
    suspend fun updateAllWidgets(context: Context) {
        Log.d(TAG, "Updating all widgets immediately")
        val startTime = System.currentTimeMillis()
        
        // 使用 Application Context
        val appContext = context.applicationContext
        
        try {
            // 并行更新所有小组件
            taskWidgetRepository.updateWidget(appContext)
            courseWidgetRepository.updateWidget(appContext)
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "All widgets updated in ${duration}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update all widgets", e)
            e.printStackTrace()
        }
    }
}
