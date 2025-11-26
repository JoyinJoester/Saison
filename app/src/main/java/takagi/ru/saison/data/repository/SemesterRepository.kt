package takagi.ru.saison.data.repository

import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.domain.model.Semester

interface SemesterRepository {
    fun getAllSemesters(): Flow<List<Semester>>
    fun getActiveSemesters(): Flow<List<Semester>>
    fun getArchivedSemesters(): Flow<List<Semester>>
    fun getSemesterById(id: Long): Flow<Semester?>
    suspend fun getSemesterByIdSync(id: Long): Semester?
    suspend fun getDefaultSemester(): Semester?
    suspend fun getLatestSemester(): Semester?
    suspend fun insertSemester(semester: Semester): Long
    suspend fun updateSemester(semester: Semester)
    suspend fun deleteSemester(id: Long)
    suspend fun updateArchiveStatus(id: Long, isArchived: Boolean)
    suspend fun getSemesterCount(): Int
    fun getActiveSemesterCount(): Flow<Int>
    suspend fun copySemester(semesterId: Long, newName: String): Long
    
    /**
     * 创建一个默认学期
     * Requirements: 1.2, 1.3, 1.4, 1.5
     * @return 新创建学期的ID
     */
    suspend fun createDefaultSemester(): Long
    
    /**
     * 检查是否存在任何学期
     * Requirements: 1.2
     * @return true如果至少有一个学期
     */
    suspend fun hasSemesters(): Boolean
}
