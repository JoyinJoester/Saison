package takagi.ru.saison.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import takagi.ru.saison.data.local.database.dao.CourseDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 默认学期初始化器实现
 * 使用Mutex确保并发安全
 * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4
 */
@Singleton
class DefaultSemesterInitializerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val semesterRepository: SemesterRepository,
    private val courseDao: CourseDao
) : DefaultSemesterInitializer {
    
    private val mutex = Mutex()
    
    companion object {
        private const val TAG = "DefaultSemesterInit"
        private const val PREFS_NAME = "default_semester_prefs"
        private const val KEY_INITIALIZED = "initialized"
    }
    
    /**
     * 确保数据库中至少存在一个学期
     * 使用Mutex确保同一时间只有一个创建操作
     * 使用SharedPreferences缓存初始化状态以提高性能
     * Requirements: 2.1, 2.2, 2.3, 2.4
     */
    override suspend fun ensureDefaultSemester() {
        // 快速检查：如果已经初始化过，直接返回
        // Requirements: 2.4 (性能优化)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_INITIALIZED, false)) {
            Log.d(TAG, "Already initialized (cached), skipping check")
            return
        }
        
        mutex.withLock {
            try {
                val startTime = System.currentTimeMillis()
                
                // 双重检查：再次验证是否已存在学期
                if (semesterRepository.hasSemesters()) {
                    Log.d(TAG, "Semesters already exist, marking as initialized")
                    // 标记为已初始化
                    prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
                    return
                }
                
                Log.d(TAG, "No semesters found, creating default semester")
                
                // 创建默认学期
                val semesterId = createDefaultSemester()
                
                // 关联孤立课程
                linkOrphanedCourses(semesterId)
                
                // 标记为已初始化
                prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Default semester created successfully with ID: $semesterId (took ${duration}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ensure default semester", e)
                // 不抛出异常，避免阻塞应用启动
            }
        }
    }
    
    /**
     * 创建默认学期
     * Requirements: 2.1, 2.2, 2.3
     * @return 新创建学期的ID
     */
    private suspend fun createDefaultSemester(): Long {
        return semesterRepository.createDefaultSemester()
    }
    
    /**
     * 关联孤立课程到默认学期
     * Requirements: 3.1, 3.2, 3.3, 3.4
     * @param semesterId 默认学期ID
     */
    private suspend fun linkOrphanedCourses(semesterId: Long) {
        try {
            // 查询所有孤立课程
            val orphanedCourses = courseDao.getOrphanedCourses()
            
            if (orphanedCourses.isEmpty()) {
                Log.d(TAG, "No orphaned courses found")
                return
            }
            
            Log.d(TAG, "Found ${orphanedCourses.size} orphaned courses, linking to semester $semesterId")
            
            // 批量更新课程的semesterId
            val courseIds = orphanedCourses.map { it.id }
            courseDao.updateCourseSemester(courseIds, semesterId)
            
            Log.d(TAG, "Successfully linked ${courseIds.size} orphaned courses to default semester")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to link orphaned courses", e)
            // 不抛出异常，即使关联失败也不影响默认学期的创建
        }
    }
}
