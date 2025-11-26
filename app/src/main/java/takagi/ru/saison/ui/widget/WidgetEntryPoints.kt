package takagi.ru.saison.ui.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 小组件EntryPoint集合
 * 
 * 由于Glance小组件无法直接使用构造函数注入，我们使用Hilt EntryPoint
 * 来访问依赖项。这个文件集中定义了所有小组件相关的EntryPoint。
 * 
 * 使用方式：
 * ```kotlin
 * val app = context.applicationContext as? SaisonApplication
 * val entryPoint = EntryPointAccessors.fromApplication(
 *     app,
 *     WidgetEntryPoints.TaskWidget::class.java
 * )
 * val repository = entryPoint.taskWidgetRepository()
 * ```
 */
object WidgetEntryPoints {
    
    /**
     * Task Widget EntryPoint
     * 提供任务小组件所需的依赖
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskWidget {
        fun taskWidgetRepository(): takagi.ru.saison.data.repository.TaskWidgetRepository
        fun taskRepository(): takagi.ru.saison.data.repository.TaskRepository
    }
    
    /**
     * Course Widget EntryPoint
     * 提供课程小组件所需的依赖
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CourseWidget {
        fun courseWidget(): takagi.ru.saison.ui.widget.CourseWidget
        fun courseWidgetRepository(): takagi.ru.saison.data.repository.CourseWidgetRepository
    }
    
    /**
     * Widget Update Coordinator EntryPoint
     * 提供小组件更新协调器
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateCoordinator {
        fun widgetUpdateCoordinator(): WidgetUpdateCoordinator
    }
}
