package takagi.ru.saison.domain.usecase.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.PomodoroRepository
import takagi.ru.saison.data.repository.RoutineRepository
import takagi.ru.saison.data.repository.SemesterRepository
import takagi.ru.saison.data.repository.SubscriptionRepository
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.data.repository.backup.WebDavBackupRepository
import takagi.ru.saison.domain.mapper.toEntity
import takagi.ru.saison.domain.model.backup.BackupFile
import takagi.ru.saison.domain.model.backup.RestoreSummary
import takagi.ru.saison.domain.repository.EventRepository
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: WebDavBackupRepository,
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository,
    private val eventRepository: EventRepository,
    private val routineRepository: RoutineRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val semesterRepository: SemesterRepository
) {
    
    suspend operator fun invoke(backupFile: BackupFile): Result<RestoreSummary> {
        return try {
            // 下载并解析备份
            val contentResult = backupRepository.downloadAndRestoreBackup(backupFile)
            if (contentResult.isFailure) {
                return Result.failure(contentResult.exceptionOrNull() ?: Exception("下载失败"))
            }
            
            val content = contentResult.getOrNull() ?: return Result.failure(Exception("备份内容为空"))
            
            var tasksImported = 0
            var coursesImported = 0
            var eventsImported = 0
            var routinesImported = 0
            var subscriptionsImported = 0
            var pomodoroSessionsImported = 0
            var semestersImported = 0
            
            // 导入任务（检测重复）
            withContext(Dispatchers.IO) {
                content.tasks.forEach { task ->
                    val exists = taskRepository.getTaskById(task.id) != null
                    if (!exists) {
                        taskRepository.insertTask(task)
                        tasksImported++
                    }
                }
                
                // 导入课程（检测重复）
                content.courses.forEach { course ->
                    val exists = courseRepository.getCourseById(course.id) != null
                    if (!exists) {
                        courseRepository.insertCourse(course)
                        coursesImported++
                    }
                }
                
                // 导入事件（检测重复）
                content.events.forEach { event ->
                    val exists = eventRepository.getEventByIdSync(event.id) != null
                    if (!exists) {
                        eventRepository.insertEvent(event)
                        eventsImported++
                    }
                }
                
                // 导入例行任务（检测重复）
                content.routines.forEach { routine ->
                    val exists = routineRepository.getRoutineTask(routine.id) != null
                    if (!exists) {
                        routineRepository.createRoutineTask(routine)
                        routinesImported++
                    }
                }
                
                // 导入订阅（检测重复）
                content.subscriptions.forEach { subscription ->
                    val exists = subscriptionRepository.getSubscriptionById(subscription.id) != null
                    if (!exists) {
                        subscriptionRepository.insertSubscription(subscription.toEntity())
                        subscriptionsImported++
                    }
                }
                
                // 导入番茄钟会话（检测重复）
                content.pomodoroSessions.forEach { session ->
                    val exists = pomodoroRepository.getSessionById(session.id) != null
                    if (!exists) {
                        pomodoroRepository.insertSession(session)
                        pomodoroSessionsImported++
                    }
                }
                
                // 导入学期（检测重复）
                content.semesters.forEach { semester ->
                    val exists = semesterRepository.getSemesterByIdSync(semester.id) != null
                    if (!exists) {
                        semesterRepository.insertSemester(semester)
                        semestersImported++
                    }
                }
            }
            
            // TODO: 导入偏好设置
            
            val summary = RestoreSummary(
                importedTasks = tasksImported,
                importedCourses = coursesImported,
                importedEvents = eventsImported,
                importedRoutines = routinesImported,
                importedSubscriptions = subscriptionsImported,
                importedPomodoroSessions = pomodoroSessionsImported,
                importedSemesters = semestersImported
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
