package takagi.ru.saison.data.local.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.data.local.database.entities.SemesterEntity

@Dao
interface SemesterDao {
    
    @Query("SELECT * FROM semesters ORDER BY startDate DESC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>
    
    @Query("SELECT * FROM semesters WHERE isArchived = 0 ORDER BY startDate DESC")
    fun getActiveSemesters(): Flow<List<SemesterEntity>>
    
    @Query("SELECT * FROM semesters WHERE isArchived = 1 ORDER BY startDate DESC")
    fun getArchivedSemesters(): Flow<List<SemesterEntity>>
    
    @Query("SELECT * FROM semesters WHERE id = :id")
    fun getSemesterById(id: Long): Flow<SemesterEntity?>
    
    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterByIdSync(id: Long): SemesterEntity?
    
    @Query("SELECT * FROM semesters WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSemester(): SemesterEntity?
    
    @Query("SELECT * FROM semesters ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestSemester(): SemesterEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesters(semesters: List<SemesterEntity>): List<Long>
    
    @Update
    suspend fun updateSemester(semester: SemesterEntity)
    
    @Delete
    suspend fun deleteSemester(semester: SemesterEntity)
    
    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteSemesterById(id: Long)
    
    @Query("UPDATE semesters SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchiveStatus(id: Long, isArchived: Boolean)
    
    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun getSemesterCount(): Int
    
    @Query("SELECT COUNT(*) FROM semesters WHERE isArchived = 0")
    fun getActiveSemesterCount(): Flow<Int>
    
    /**
     * 检查是否存在任何学期
     * Requirements: 3.1
     */
    @Query("SELECT EXISTS(SELECT 1 FROM semesters LIMIT 1)")
    suspend fun hasSemesters(): Boolean
}
