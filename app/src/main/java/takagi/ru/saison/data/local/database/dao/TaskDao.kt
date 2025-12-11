package takagi.ru.saison.data.local.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.data.local.database.entities.TaskEntity
import takagi.ru.saison.data.local.database.entities.relations.TaskWithDetails
import takagi.ru.saison.data.local.database.entities.relations.TaskWithSubtasks

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL ORDER BY dueDate ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL")
    suspend fun getAllTasksList(): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: Long): Flow<TaskEntity?>
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskWithSubtasks(taskId: Long): Flow<TaskWithSubtasks>
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithDetails(taskId: Long): TaskWithDetails?
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND parentTaskId IS NULL ORDER BY priority DESC, dueDate ASC")
    fun getIncompleteTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND parentTaskId IS NULL ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startDate AND :endDate AND parentTaskId IS NULL ORDER BY dueDate ASC")
    fun getTasksByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId AND parentTaskId IS NULL ORDER BY dueDate ASC")
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE priority = :priority AND isCompleted = 0 AND parentTaskId IS NULL ORDER BY dueDate ASC")
    fun getTasksByPriority(priority: Int): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE updatedAt > :timestamp")
    suspend fun getTasksUpdatedAfter(timestamp: Long): List<TaskEntity>

    @Query("UPDATE tasks SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun moveTasksToCategory(oldCategoryId: Long, newCategoryId: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>
    
    @Update
    suspend fun update(task: TaskEntity)
    
    @Update
    suspend fun updateAll(tasks: List<TaskEntity>)
    
    @Delete
    suspend fun delete(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
    
    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND completedAt < :timestamp")
    suspend fun deleteCompletedBefore(timestamp: Long)
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateCompletionStatus(taskId: Long, isCompleted: Boolean, completedAt: Long?)
    
    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: Long, status: Int)
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND parentTaskId IS NULL")
    fun getIncompleteTaskCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE dueDate < :timestamp AND isCompleted = 0 AND parentTaskId IS NULL")
    fun getOverdueTaskCount(timestamp: Long): Flow<Int>
}
