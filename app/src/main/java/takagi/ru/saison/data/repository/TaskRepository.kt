package takagi.ru.saison.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import takagi.ru.saison.data.local.database.dao.TaskDao
import takagi.ru.saison.data.local.database.dao.TagDao
import takagi.ru.saison.data.local.encryption.EncryptionManager
import takagi.ru.saison.domain.mapper.toDomain
import takagi.ru.saison.domain.mapper.toEntity
import takagi.ru.saison.domain.model.Task
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val encryptionManager: EncryptionManager,
    @javax.inject.Named("applicationContext") private val context: android.content.Context,
    private val notificationManager: takagi.ru.saison.notification.SaisonNotificationManager
) {
    
    // Lazy inject to avoid circular dependency
    private val widgetUpdateCoordinator: takagi.ru.saison.ui.widget.WidgetUpdateCoordinator by lazy {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context,
            takagi.ru.saison.ui.widget.WidgetEntryPoints.UpdateCoordinator::class.java
        ).widgetUpdateCoordinator()
    }
    
    fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getIncompleteTasks(): Flow<List<Task>> {
        return taskDao.getIncompleteTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTasksByDateRange(startDate: Long, endDate: Long): Flow<List<Task>> {
        return taskDao.getTasksByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTasksByCategory(categoryId: Long): Flow<List<Task>> {
        return taskDao.getTasksByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTasksByPriority(priority: Int): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }
    
    fun getTaskByIdFlow(taskId: Long): Flow<Task?> {
        return taskDao.getTaskByIdFlow(taskId).map { it?.toDomain() }
    }
    
    suspend fun getTaskWithSubtasks(taskId: Long): Task? {
        val taskWithSubtasks = taskDao.getTaskWithDetails(taskId) ?: return null
        val task = taskWithSubtasks.task.toDomain()
        val subtasks = taskWithSubtasks.subtasks.map { it.toDomain() }
        
        return task.copy(
            subtasks = subtasks,
            category = taskWithSubtasks.category?.toDomain()
        )
    }
    
    suspend fun insertTask(task: Task): Long {
        // Get or create default category
        val categoryId = task.category?.id ?: getOrCreateDefaultCategory()
        val entity = task.toEntity(categoryId)
        val result = taskDao.insert(entity)
        
        // Schedule notification if task has a due date
        try {
            val taskWithId = task.copy(id = result)
            if (taskWithId.dueDate != null) {
                notificationManager.scheduleTaskReminder(taskWithId)
                android.util.Log.d("TaskRepository", "Task reminder scheduled for task $result")
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to schedule task reminder", e)
        }
        
        // Trigger widget update
        try {
            widgetUpdateCoordinator.onTaskChanged(context)
            android.util.Log.d("TaskRepository", "Widget update triggered after task insert")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to trigger widget update", e)
        }
        
        return result
    }
    
    suspend fun updateTask(task: Task) {
        val categoryId = task.category?.id ?: getOrCreateDefaultCategory()
        val entity = task.toEntity(categoryId)
        taskDao.update(entity)
        
        // Update notification if task has a due date, otherwise cancel it
        try {
            if (task.dueDate != null && !task.isCompleted) {
                notificationManager.scheduleTaskReminder(task)
                android.util.Log.d("TaskRepository", "Task reminder updated for task ${task.id}")
            } else {
                notificationManager.cancelTaskReminder(task.id)
                android.util.Log.d("TaskRepository", "Task reminder cancelled for task ${task.id}")
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to update task reminder", e)
        }
        
        // Trigger widget update
        try {
            widgetUpdateCoordinator.onTaskChanged(context)
            android.util.Log.d("TaskRepository", "Widget update triggered after task update")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to trigger widget update", e)
        }
    }
    
    suspend fun deleteTask(taskId: Long) {
        // Cancel notification before deleting task
        try {
            notificationManager.cancelTaskReminder(taskId)
            android.util.Log.d("TaskRepository", "Task reminder cancelled for deleted task $taskId")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to cancel task reminder", e)
        }
        
        taskDao.deleteById(taskId)
        
        // Trigger widget update
        try {
            widgetUpdateCoordinator.onTaskChanged(context)
            android.util.Log.d("TaskRepository", "Widget update triggered after task delete")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to trigger widget update", e)
        }
    }
    
    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        taskDao.updateCompletionStatus(taskId, isCompleted, completedAt)
        
        // Cancel notification when task is completed, reschedule when uncompleted
        try {
            if (isCompleted) {
                notificationManager.cancelTaskReminder(taskId)
                android.util.Log.d("TaskRepository", "Task reminder cancelled for completed task $taskId")
            } else {
                // Reschedule notification if task is uncompleted and has a due date
                val task = getTaskById(taskId)
                if (task?.dueDate != null) {
                    notificationManager.scheduleTaskReminder(task)
                    android.util.Log.d("TaskRepository", "Task reminder rescheduled for uncompleted task $taskId")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to update task reminder on completion toggle", e)
        }
        
        // Trigger widget update
        try {
            widgetUpdateCoordinator.onTaskChanged(context)
            android.util.Log.d("TaskRepository", "Widget update triggered after task completion toggle")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to trigger widget update", e)
        }
    }
    
    suspend fun deleteCompletedTasksOlderThan(days: Int) {
        val timestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        taskDao.deleteCompletedBefore(timestamp)
    }
    
    fun getIncompleteTaskCount(): Flow<Int> {
        return taskDao.getIncompleteTaskCount()
    }
    
    fun getOverdueTaskCount(): Flow<Int> {
        return taskDao.getOverdueTaskCount(System.currentTimeMillis())
    }
    
    private suspend fun getOrCreateDefaultCategory(): Long {
        val defaultTag = tagDao.getTagByPath("default")
        if (defaultTag != null) {
            return defaultTag.id
        }
        
        // Create default category
        val newTag = takagi.ru.saison.data.local.database.entities.TagEntity(
            name = "Default",
            path = "default",
            parentId = null,
            icon = null,
            color = 0xFF6200EE.toInt()
        )
        return tagDao.insert(newTag)
    }

    fun getAllTags(): Flow<List<takagi.ru.saison.domain.model.Tag>> {
        return tagDao.getAllTagsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addTag(name: String): Long {
        val newTag = takagi.ru.saison.data.local.database.entities.TagEntity(
            name = name,
            path = name,
            parentId = null,
            icon = null,
            color = 0xFF6200EE.toInt()
        )
        return tagDao.insert(newTag)
    }

    suspend fun renameTag(tagId: Long, newName: String) {
        val tag = tagDao.getTagById(tagId)
        if (tag != null) {
            val updatedTag = tag.copy(name = newName, path = newName)
            tagDao.update(updatedTag)
        }
    }

    suspend fun deleteTag(tagId: Long) {
        val defaultTagId = getOrCreateDefaultCategory()
        if (tagId == defaultTagId) return

        // Move tasks to default category
        taskDao.moveTasksToCategory(tagId, defaultTagId)
        
        // Delete tag
        tagDao.deleteById(tagId)
    }
}

// WidgetUpdateCoordinatorEntryPoint moved to WidgetEntryPoints.kt for centralized management
