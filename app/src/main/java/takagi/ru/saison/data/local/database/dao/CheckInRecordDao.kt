package takagi.ru.saison.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.data.local.database.entity.CheckInRecordEntity

/**
 * 打卡记录数据访问对象
 */
@Dao
interface CheckInRecordDao {
    
    /**
     * 插入新的打卡记录
     * @return 插入的记录 ID
     */
    @Insert
    suspend fun insert(record: CheckInRecordEntity): Long
    
    /**
     * 删除打卡记录
     */
    @Delete
    suspend fun delete(record: CheckInRecordEntity)
    
    /**
     * 根据任务 ID 获取所有打卡记录
     * 按打卡时间倒序排列
     */
    @Query("SELECT * FROM check_in_records WHERE routine_task_id = :taskId ORDER BY check_in_time DESC")
    fun getByTaskId(taskId: Long): Flow<List<CheckInRecordEntity>>
    
    /**
     * 根据任务 ID 获取所有打卡记录（一次性查询，不使用 Flow）
     * 按打卡时间倒序排列
     */
    @Query("SELECT * FROM check_in_records WHERE routine_task_id = :taskId ORDER BY check_in_time DESC")
    suspend fun getByTaskIdOnce(taskId: Long): List<CheckInRecordEntity>
    
    /**
     * 根据任务 ID 和周期范围获取打卡记录
     * @param taskId 任务 ID
     * @param cycleStart 周期开始日期（epoch day）
     * @param cycleEnd 周期结束日期（epoch day）
     */
    @Query("""
        SELECT * FROM check_in_records 
        WHERE routine_task_id = :taskId 
        AND cycle_start_date = :cycleStart 
        AND cycle_end_date = :cycleEnd
        ORDER BY check_in_time DESC
    """)
    fun getByCycle(
        taskId: Long, 
        cycleStart: Long, 
        cycleEnd: Long
    ): Flow<List<CheckInRecordEntity>>
    
    /**
     * 统计指定周期内的打卡次数
     * @param taskId 任务 ID
     * @param cycleStart 周期开始日期（epoch day）
     * @param cycleEnd 周期结束日期（epoch day）
     * @return 打卡次数
     */
    @Query("""
        SELECT COUNT(*) FROM check_in_records 
        WHERE routine_task_id = :taskId 
        AND cycle_start_date = :cycleStart 
        AND cycle_end_date = :cycleEnd
    """)
    suspend fun getCountInCycle(
        taskId: Long, 
        cycleStart: Long, 
        cycleEnd: Long
    ): Int
    
    /**
     * 获取任务的最后一次打卡记录
     */
    @Query("""
        SELECT * FROM check_in_records 
        WHERE routine_task_id = :taskId 
        ORDER BY check_in_time DESC 
        LIMIT 1
    """)
    suspend fun getLastCheckIn(taskId: Long): CheckInRecordEntity?
    
    /**
     * 根据 ID 删除打卡记录
     */
    @Query("DELETE FROM check_in_records WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 删除指定任务的所有打卡记录
     */
    @Query("DELETE FROM check_in_records WHERE routine_task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
    
    /**
     * 获取所有打卡记录（用于监听变化）
     * 按打卡时间倒序排列
     */
    @Query("SELECT * FROM check_in_records ORDER BY check_in_time DESC")
    fun getAllCheckIns(): Flow<List<CheckInRecordEntity>>
}
