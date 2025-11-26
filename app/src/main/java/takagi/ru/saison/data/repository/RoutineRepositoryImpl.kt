package takagi.ru.saison.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import takagi.ru.saison.data.local.database.dao.CheckInRecordDao
import takagi.ru.saison.data.local.database.dao.RoutineTaskDao
import takagi.ru.saison.domain.mapper.RoutineMapper
import takagi.ru.saison.domain.model.routine.CheckInRecord
import takagi.ru.saison.domain.model.routine.RoutineTask
import takagi.ru.saison.domain.model.routine.RoutineTaskWithStats
import takagi.ru.saison.util.CycleCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 周期性任务仓库实现
 */
@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineTaskDao: RoutineTaskDao,
    private val checkInRecordDao: CheckInRecordDao,
    private val cycleCalculator: CycleCalculator
) : RoutineRepository {
    
    // ========== 任务管理 ==========
    
    override suspend fun createRoutineTask(task: RoutineTask): Long {
        return try {
            val entity = RoutineMapper.toEntity(task)
            routineTaskDao.insert(entity)
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to create routine task", e)
        }
    }
    
    override suspend fun updateRoutineTask(task: RoutineTask) {
        try {
            val entity = RoutineMapper.toEntity(task)
            routineTaskDao.update(entity)
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to update routine task", e)
        }
    }
    
    override suspend fun deleteRoutineTask(taskId: Long) {
        try {
            routineTaskDao.deleteById(taskId)
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to delete routine task", e)
        }
    }
    
    override suspend fun getRoutineTask(taskId: Long): RoutineTask? {
        return try {
            routineTaskDao.getById(taskId)?.let { RoutineMapper.toDomain(it) }
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to get routine task", e)
        }
    }
    
    override fun getAllRoutineTasks(): Flow<List<RoutineTask>> {
        return routineTaskDao.getAllActive().map { entities ->
            entities.map { RoutineMapper.toDomain(it) }
        }
    }
    
    override fun getRoutineTasksWithDuration(): Flow<List<RoutineTask>> {
        return routineTaskDao.getAllActive().map { entities ->
            entities
                .map { RoutineMapper.toDomain(it) }
                .filter { it.durationMinutes != null && it.durationMinutes > 0 }
        }
    }
    
    // ========== 打卡管理 ==========
    
    override suspend fun checkIn(taskId: Long, note: String?): CheckInRecord {
        return try {
            // 获取任务
            val task = getRoutineTask(taskId)
                ?: throw RoutineRepositoryException.TaskNotFound("Task not found: $taskId")
            
            // 检查任务是否在活跃周期内
            if (!cycleCalculator.isInActiveCycle(task)) {
                throw RoutineRepositoryException.NotInActiveCycle("Task is not in active cycle")
            }
            
            // 获取当前周期范围
            val cycle = cycleCalculator.getCurrentCycle(task)
                ?: throw RoutineRepositoryException.CycleCalculationError("Failed to calculate current cycle")
            
            // 创建打卡记录
            val checkInRecord = CheckInRecord(
                id = 0,
                routineTaskId = taskId,
                checkInTime = LocalDateTime.now(),
                note = note,
                cycleStartDate = cycle.first,
                cycleEndDate = cycle.second
            )
            
            val entity = RoutineMapper.toEntity(checkInRecord)
            val recordId = checkInRecordDao.insert(entity)
            
            checkInRecord.copy(id = recordId)
        } catch (e: RoutineRepositoryException) {
            throw e
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to check in", e)
        }
    }
    
    override suspend fun checkInWithNote(taskId: Long, note: String): CheckInRecord {
        return checkIn(taskId, note)
    }
    
    override suspend fun deleteCheckIn(checkInId: Long) {
        try {
            checkInRecordDao.deleteById(checkInId)
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to delete check-in record", e)
        }
    }
    
    override fun getCheckInRecords(taskId: Long): Flow<List<CheckInRecord>> {
        return checkInRecordDao.getByTaskId(taskId).map { entities ->
            entities.map { RoutineMapper.toDomain(it) }
        }
    }
    
    override suspend fun getCheckInRecordsOnce(taskId: Long): List<CheckInRecord> {
        return try {
            checkInRecordDao.getByTaskIdOnce(taskId).map { RoutineMapper.toDomain(it) }
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to get check-in records", e)
        }
    }
    
    override fun getCheckInRecordsInCycle(
        taskId: Long,
        cycleStart: LocalDate,
        cycleEnd: LocalDate
    ): Flow<List<CheckInRecord>> {
        return checkInRecordDao.getByCycle(
            taskId = taskId,
            cycleStart = cycleStart.toEpochDay(),
            cycleEnd = cycleEnd.toEpochDay()
        ).map { entities ->
            entities.map { RoutineMapper.toDomain(it) }
        }
    }
    
    // ========== 统计查询 ==========
    
    override fun getRoutineTasksWithStats(): Flow<List<RoutineTaskWithStats>> {
        // 监听所有打卡记录的变化，以便在打卡后刷新统计
        return combine(
            getAllRoutineTasks(),
            checkInRecordDao.getAllCheckIns()
        ) { tasks, checkIns ->
            // 为每个任务创建统计信息
            // 使用 checkIns 来触发重新计算
            tasks.map { task ->
                val isInActiveCycle = cycleCalculator.isInActiveCycle(task)
                val currentCycle = cycleCalculator.getCurrentCycle(task)
                val nextActiveDate = if (!isInActiveCycle) {
                    cycleCalculator.getNextActiveDate(task)
                } else null
                
                // 从 checkIns 中计算当前周期的打卡次数
                val checkInCount = if (currentCycle != null) {
                    checkIns.count { record ->
                        record.routineTaskId == task.id &&
                        record.cycleStartDate == currentCycle.first.toEpochDay() &&
                        record.cycleEndDate == currentCycle.second.toEpochDay()
                    }
                } else 0
                
                // 获取最后一次打卡
                val lastCheckIn = checkIns
                    .filter { it.routineTaskId == task.id }
                    .maxByOrNull { it.checkInTime }
                    ?.let { RoutineMapper.toDomain(it) }
                
                RoutineTaskWithStats(
                    task = task,
                    checkInCount = checkInCount,
                    isInActiveCycle = isInActiveCycle,
                    currentCycleStart = currentCycle?.first,
                    currentCycleEnd = currentCycle?.second,
                    nextActiveDate = nextActiveDate,
                    lastCheckInTime = lastCheckIn?.checkInTime
                )
            }
        }
    }
    
    override suspend fun getCheckInCountInCycle(
        taskId: Long,
        cycleStart: LocalDate,
        cycleEnd: LocalDate
    ): Int {
        return try {
            checkInRecordDao.getCountInCycle(
                taskId = taskId,
                cycleStart = cycleStart.toEpochDay(),
                cycleEnd = cycleEnd.toEpochDay()
            )
        } catch (e: Exception) {
            throw RoutineRepositoryException.DatabaseError("Failed to get check-in count", e)
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 创建带统计信息的任务
     */
    private suspend fun createTaskWithStats(task: RoutineTask): RoutineTaskWithStats {
        val isInActiveCycle = cycleCalculator.isInActiveCycle(task)
        val currentCycle = cycleCalculator.getCurrentCycle(task)
        val nextActiveDate = if (!isInActiveCycle) {
            cycleCalculator.getNextActiveDate(task)
        } else null
        
        val checkInCount = if (currentCycle != null) {
            getCheckInCountInCycle(task.id, currentCycle.first, currentCycle.second)
        } else 0
        
        val lastCheckIn = checkInRecordDao.getLastCheckIn(task.id)
            ?.let { RoutineMapper.toDomain(it) }
        
        return RoutineTaskWithStats(
            task = task,
            checkInCount = checkInCount,
            isInActiveCycle = isInActiveCycle,
            currentCycleStart = currentCycle?.first,
            currentCycleEnd = currentCycle?.second,
            nextActiveDate = nextActiveDate,
            lastCheckInTime = lastCheckIn?.checkInTime
        )
    }
}

/**
 * Repository 异常类
 */
sealed class RoutineRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatabaseError(message: String, cause: Throwable? = null) : RoutineRepositoryException(message, cause)
    class TaskNotFound(message: String) : RoutineRepositoryException(message)
    class NotInActiveCycle(message: String) : RoutineRepositoryException(message)
    class CycleCalculationError(message: String) : RoutineRepositoryException(message)
}
