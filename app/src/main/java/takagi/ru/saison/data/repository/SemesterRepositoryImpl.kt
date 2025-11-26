package takagi.ru.saison.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import takagi.ru.saison.data.local.database.dao.CourseDao
import takagi.ru.saison.data.local.database.dao.SemesterDao
import takagi.ru.saison.domain.mapper.toDomain
import takagi.ru.saison.domain.mapper.toEntity
import takagi.ru.saison.domain.model.Semester
import takagi.ru.saison.ui.widget.CourseWidgetScheduler
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class SemesterRepositoryImpl @Inject constructor(
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao,
    private val widgetScheduler: CourseWidgetScheduler
) : SemesterRepository {
    
    override fun getAllSemesters(): Flow<List<Semester>> {
        return semesterDao.getAllSemesters().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getActiveSemesters(): Flow<List<Semester>> {
        return semesterDao.getActiveSemesters().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getArchivedSemesters(): Flow<List<Semester>> {
        return semesterDao.getArchivedSemesters().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getSemesterById(id: Long): Flow<Semester?> {
        return semesterDao.getSemesterById(id).map { entity ->
            entity?.toDomain()
        }
    }
    
    override suspend fun getSemesterByIdSync(id: Long): Semester? {
        return semesterDao.getSemesterByIdSync(id)?.toDomain()
    }
    
    override suspend fun getDefaultSemester(): Semester? {
        return semesterDao.getDefaultSemester()?.toDomain()
    }
    
    override suspend fun getLatestSemester(): Semester? {
        return semesterDao.getLatestSemester()?.toDomain()
    }
    
    override suspend fun insertSemester(semester: Semester): Long {
        return semesterDao.insertSemester(semester.toEntity())
    }
    
    override suspend fun updateSemester(semester: Semester) {
        semesterDao.updateSemester(
            semester.copy(updatedAt = System.currentTimeMillis()).toEntity()
        )
        widgetScheduler.updateNow()
    }
    
    override suspend fun deleteSemester(id: Long) {
        semesterDao.deleteSemesterById(id)
    }
    
    override suspend fun updateArchiveStatus(id: Long, isArchived: Boolean) {
        semesterDao.updateArchiveStatus(id, isArchived)
    }
    
    override suspend fun getSemesterCount(): Int {
        return semesterDao.getSemesterCount()
    }
    
    override fun getActiveSemesterCount(): Flow<Int> {
        return semesterDao.getActiveSemesterCount()
    }
    
    override suspend fun copySemester(semesterId: Long, newName: String): Long {
        // 获取源学期
        val sourceSemester = semesterDao.getSemesterByIdSync(semesterId)
            ?: throw IllegalArgumentException("Source semester not found")
        
        // 创建新学期
        val newSemester = sourceSemester.copy(
            id = 0,
            name = newName,
            isArchived = false,
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newSemesterId = semesterDao.insertSemester(newSemester)
        
        // 复制所有课程
        val courses = courseDao.getAllCoursesList().filter { it.semesterId == semesterId }
        val newCourses = courses.map { course ->
            course.copy(
                id = 0,
                semesterId = newSemesterId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        courseDao.insertAll(newCourses)
        
        return newSemesterId
    }
    
    /**
     * 创建一个默认学期
     * Requirements: 1.2, 1.3, 1.4, 1.5
     */
    override suspend fun createDefaultSemester(): Long {
        val now = LocalDate.now()
        val startDate = getMondayOfCurrentWeek(now)
        val endDate = startDate.plusWeeks(18)
        
        val defaultSemester = Semester(
            id = 0,
            name = "未命名学期",
            startDate = startDate,
            endDate = endDate,
            totalWeeks = 18,
            isArchived = false,
            isDefault = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        return semesterDao.insertSemester(defaultSemester.toEntity())
    }
    
    /**
     * 检查是否存在任何学期
     * Requirements: 1.2
     */
    override suspend fun hasSemesters(): Boolean {
        return semesterDao.hasSemesters()
    }
    
    /**
     * 计算当前周的周一
     * Requirements: 1.3
     * @param date 参考日期
     * @return 该日期所在周的周一
     */
    private fun getMondayOfCurrentWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
