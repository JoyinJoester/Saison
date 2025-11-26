package takagi.ru.saison.ui.widget.model

import takagi.ru.saison.domain.model.Task

/**
 * 任务小组件显示所需的数据
 */
data class TaskWidgetData(
    val allTasks: List<WidgetTask>,  // 所有任务（包含已完成和未完成）
    val incompleteCount: Int,
    val completedTodayCount: Int
) {
    companion object {
        fun empty() = TaskWidgetData(
            allTasks = emptyList(),
            incompleteCount = 0,
            completedTodayCount = 0
        )
    }
    
    // 便捷属性：未完成任务
    val incompleteTasks: List<WidgetTask>
        get() = allTasks.filter { !it.isCompleted }
    
    // 便捷属性：已完成任务
    val completedTasks: List<WidgetTask>
        get() = allTasks.filter { it.isCompleted }
}

/**
 * 小组件任务简化模型
 */
data class WidgetTask(
    val id: Long,
    val title: String,
    val isFavorite: Boolean,
    val isCompleted: Boolean,
    val priority: Int
)

/**
 * 将 Task 转换为 WidgetTask
 */
fun Task.toWidgetTask() = WidgetTask(
    id = id,
    title = title,
    isFavorite = isFavorite,
    isCompleted = isCompleted,
    priority = priority.ordinal
)
