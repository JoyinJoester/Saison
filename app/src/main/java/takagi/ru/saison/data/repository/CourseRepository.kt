package takagi.ru.saison.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import takagi.ru.saison.data.local.database.dao.CourseDao
import takagi.ru.saison.domain.mapper.toDomain
import takagi.ru.saison.domain.mapper.toEntity
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.ui.widget.CourseWidgetScheduler
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao,
    private val widgetScheduler: CourseWidgetScheduler,
    @javax.inject.Named("applicationContext") private val context: android.content.Context
) {
    
    // Lazy inject to avoid circular dependency
    private val widgetUpdateCoordinator: takagi.ru.saison.ui.widget.WidgetUpdateCoordinator by lazy {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context,
            takagi.ru.saison.ui.widget.WidgetEntryPoints.UpdateCoordinator::class.java
        ).widgetUpdateCoordinator()
    }
    
    fun getAllCourses(): Flow<List<Course>> {
        return courseDao.getAllCoursesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getCoursesByDay(dayOfWeek: DayOfWeek): Flow<List<Course>> {
        return courseDao.getCoursesByDay(dayOfWeek.value).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getActiveCourses(currentDate: Long): Flow<List<Course>> {
        return courseDao.getActiveCourses(currentDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getCourseById(courseId: Long): Course? {
        return courseDao.getCourseById(courseId)?.toDomain()
    }
    
    suspend fun insertCourse(course: Course): Long {
        val result = courseDao.insert(course.toEntity())
        widgetScheduler.updateNow()
        
        // Trigger widget update via coordinator
        try {
            widgetUpdateCoordinator.onCourseChanged(context)
            android.util.Log.d("CourseRepository", "Widget update triggered after course insert")
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Failed to trigger widget update", e)
        }
        
        return result
    }
    
    suspend fun insertCourses(courses: List<Course>): List<Long> {
        val result = courseDao.insertAll(courses.map { it.toEntity() })
        widgetScheduler.updateNow()
        
        // Trigger widget update via coordinator
        try {
            widgetUpdateCoordinator.onCourseChanged(context)
            android.util.Log.d("CourseRepository", "Widget update triggered after courses insert")
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Failed to trigger widget update", e)
        }
        
        return result
    }
    
    suspend fun updateCourse(course: Course) {
        courseDao.update(course.toEntity())
        widgetScheduler.updateNow()
        
        // Trigger widget update via coordinator
        try {
            widgetUpdateCoordinator.onCourseChanged(context)
            android.util.Log.d("CourseRepository", "Widget update triggered after course update")
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Failed to trigger widget update", e)
        }
    }
    
    suspend fun deleteCourse(courseId: Long) {
        courseDao.deleteById(courseId)
        widgetScheduler.updateNow()
        
        // Trigger widget update via coordinator
        try {
            widgetUpdateCoordinator.onCourseChanged(context)
            android.util.Log.d("CourseRepository", "Widget update triggered after course delete")
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Failed to trigger widget update", e)
        }
    }
    
    suspend fun deleteExpiredCourses() {
        val currentTime = System.currentTimeMillis()
        courseDao.deleteExpiredCourses(currentTime)
    }
    
    // Semester-related methods
    fun getCoursesBySemester(semesterId: Long): Flow<List<Course>> {
        return courseDao.getCoursesBySemester(semesterId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getCourseCountBySemester(semesterId: Long): Int {
        return courseDao.getCourseCountBySemester(semesterId)
    }
    
    suspend fun moveCourseToSemester(courseId: Long, semesterId: Long) {
        courseDao.moveCourseToSemester(courseId, semesterId)
    }
    
    suspend fun deleteCoursesBySemester(semesterId: Long) {
        courseDao.deleteCoursesBySemester(semesterId)
    }
}
