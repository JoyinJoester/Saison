package takagi.ru.saison.data.local.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.data.local.database.entities.CourseEntity

@Dao
interface CourseDao {
    
    @Query("SELECT * FROM courses ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllCoursesFlow(): Flow<List<CourseEntity>>
    
    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesList(): List<CourseEntity>
    
    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Long): CourseEntity?
    
    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getCoursesByDay(dayOfWeek: Int): Flow<List<CourseEntity>>
    
    @Query("SELECT * FROM courses WHERE :currentDate BETWEEN startDate AND endDate ORDER BY dayOfWeek ASC, startTime ASC")
    fun getActiveCourses(currentDate: Long): Flow<List<CourseEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>): List<Long>
    
    @Update
    suspend fun update(course: CourseEntity)
    
    @Delete
    suspend fun delete(course: CourseEntity)
    
    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteById(courseId: Long)
    
    @Query("DELETE FROM courses WHERE endDate < :timestamp")
    suspend fun deleteExpiredCourses(timestamp: Long)
    
    // Semester-related queries
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek ASC, startTime ASC")
    fun getCoursesBySemester(semesterId: Long): Flow<List<CourseEntity>>
    
    @Query("SELECT COUNT(*) FROM courses WHERE semesterId = :semesterId")
    suspend fun getCourseCountBySemester(semesterId: Long): Int
    
    @Query("UPDATE courses SET semesterId = :semesterId WHERE id = :courseId")
    suspend fun moveCourseToSemester(courseId: Long, semesterId: Long)
    
    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteCoursesBySemester(semesterId: Long)
    
    /**
     * 查询所有semesterId指向不存在学期的课程（孤立课程）
     * Requirements: 3.1
     */
    @Query("""
        SELECT * FROM courses 
        WHERE semesterId NOT IN (SELECT id FROM semesters)
    """)
    suspend fun getOrphanedCourses(): List<CourseEntity>
    
    /**
     * 批量更新课程的semesterId
     * Requirements: 3.2
     */
    @Query("UPDATE courses SET semesterId = :semesterId WHERE id IN (:courseIds)")
    suspend fun updateCourseSemester(courseIds: List<Long>, semesterId: Long)
}
