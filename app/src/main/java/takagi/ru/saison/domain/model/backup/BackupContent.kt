package takagi.ru.saison.domain.model.backup

import takagi.ru.saison.util.backup.CategoryBackupDto
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Event
import takagi.ru.saison.domain.model.PomodoroSession
import takagi.ru.saison.domain.model.routine.RoutineTask
import takagi.ru.saison.domain.model.Semester
import takagi.ru.saison.domain.model.Subscription
import takagi.ru.saison.domain.model.Task

data class BackupContent(
    val tasks: List<Task> = emptyList(),
    val courses: List<Course> = emptyList(),
    val events: List<Event> = emptyList(),
    val routines: List<RoutineTask> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val pomodoroSessions: List<PomodoroSession> = emptyList(),
    val semesters: List<Semester> = emptyList(),
    val preferences: Map<String, Any> = emptyMap(),
    val categories: List<CategoryBackupDto> = emptyList()
)
