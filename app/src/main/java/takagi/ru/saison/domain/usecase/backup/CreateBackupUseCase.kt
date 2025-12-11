package takagi.ru.saison.domain.usecase.backup

import kotlinx.coroutines.flow.first
import takagi.ru.saison.data.repository.CategoryRepository
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.PomodoroRepository
import takagi.ru.saison.data.repository.RoutineRepository
import takagi.ru.saison.data.repository.SemesterRepository
import takagi.ru.saison.data.repository.SubscriptionRepository
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.data.repository.backup.WebDavBackupRepository
import takagi.ru.saison.domain.mapper.toDomain
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.repository.EventRepository
import takagi.ru.saison.util.backup.DataExporter
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    private val backupRepository: WebDavBackupRepository,
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository,
    private val eventRepository: EventRepository,
    private val routineRepository: RoutineRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val semesterRepository: SemesterRepository,
    private val categoryRepository: CategoryRepository,
    private val dataExporter: DataExporter
) {
    
    suspend operator fun invoke(preferences: BackupPreferences): Result<String> {
        return try {
            android.util.Log.d("CreateBackupUseCase", "开始备份流程")
            
            // 验证至少选择了一种数据类型
            if (!preferences.hasAnyEnabled()) {
                android.util.Log.e("CreateBackupUseCase", "没有选择任何备份内容")
                return Result.failure(Exception("请至少选择一种备份内容"))
            }
            
            // 验证 WebDAV 已配置
            if (!backupRepository.isConfigured()) {
                android.util.Log.e("CreateBackupUseCase", "WebDAV 未配置")
                return Result.failure(Exception("未配置 WebDAV"))
            }
            
            android.util.Log.d("CreateBackupUseCase", "开始收集数据")
            // 收集数据并导出为 JSON
            val files = mutableMapOf<String, String>()
            
            if (preferences.includeTasks) {
                val tasks = taskRepository.getAllTasks().first()
                android.util.Log.d("CreateBackupUseCase", "导出 ${tasks.size} 个任务")
                files["tasks.json"] = dataExporter.exportTasks(tasks)
            }
            
            if (preferences.includeCourses) {
                val courses = courseRepository.getAllCourses().first()
                android.util.Log.d("CreateBackupUseCase", "导出 ${courses.size} 个课程")
                files["courses.json"] = dataExporter.exportCourses(courses)
            }
            
            if (preferences.includeEvents) {
                val events = eventRepository.getAllEvents().first()
                android.util.Log.d("CreateBackupUseCase", "导出 ${events.size} 个事件")
                files["events.json"] = dataExporter.exportEvents(events)
            }
            
            if (preferences.includeRoutines) {
                val routines = routineRepository.getAllRoutineTasks().first()
                android.util.Log.d("CreateBackupUseCase", "导出 ${routines.size} 个日程")
                files["routines.json"] = dataExporter.exportRoutines(routines)
            }
            
            if (preferences.includeSubscriptions) {
                val subscriptionEntities = subscriptionRepository.getAllSubscriptions().first()
                val subscriptions = subscriptionEntities.map { it.toDomain() }
                android.util.Log.d("CreateBackupUseCase", "导出 ${subscriptions.size} 个订阅")
                files["subscriptions.json"] = dataExporter.exportSubscriptions(subscriptions)
            }
            
            if (preferences.includePomodoroSessions) {
                val sessions = pomodoroRepository.getAllSessions().first()
                android.util.Log.d("CreateBackupUseCase", "导出 ${sessions.size} 个番茄钟")
                files["pomodoro_sessions.json"] = dataExporter.exportPomodoroSessions(sessions)
            }
            
            if (preferences.includeSemesters) {
                val semesters = semesterRepository.getAllSemesters().first()
                android.util.Log.d("CreateBackupUseCase", "导出 ${semesters.size} 个学期")
                files["semesters.json"] = dataExporter.exportSemesters(semesters)
            }
            
            // 始终导出分类数据（用于订阅功能）
            val categories = categoryRepository.getAllCategories().first()
            if (categories.isNotEmpty()) {
                android.util.Log.d("CreateBackupUseCase", "导出 ${categories.size} 个分类")
                files["categories.json"] = dataExporter.exportCategories(categories)
            }
            
            if (preferences.includePreferences) {
                // TODO: 实现偏好设置导出
                files["preferences.json"] = dataExporter.exportPreferences(emptyMap())
            }
            
            android.util.Log.d("CreateBackupUseCase", "共收集 ${files.size} 个文件，开始上传")
            // 调用 repository 创建并上传备份
            val result = backupRepository.createAndUploadBackup(preferences, files)
            android.util.Log.d("CreateBackupUseCase", "上传结果: ${result.isSuccess}")
            result
        } catch (e: Exception) {
            android.util.Log.e("CreateBackupUseCase", "备份异常", e)
            Result.failure(e)
        }
    }
}
