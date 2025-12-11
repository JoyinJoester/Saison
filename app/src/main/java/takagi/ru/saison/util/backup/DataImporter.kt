package takagi.ru.saison.util.backup

import kotlinx.serialization.json.Json
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.Event
import takagi.ru.saison.domain.model.PomodoroSession
import takagi.ru.saison.domain.model.Semester
import takagi.ru.saison.domain.model.Subscription
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.model.routine.RoutineTask
import javax.inject.Inject

class DataImporter @Inject constructor(
    private val json: Json
) {
    
    fun importTasks(jsonString: String): List<Task> {
        return try {
            json.decodeFromString<List<Task>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importCourses(jsonString: String): List<Course> {
        return try {
            json.decodeFromString<List<Course>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importEvents(jsonString: String): List<Event> {
        return try {
            json.decodeFromString<List<Event>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importRoutines(jsonString: String): List<RoutineTask> {
        return try {
            json.decodeFromString<List<RoutineTask>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importSubscriptions(jsonString: String): List<Subscription> {
        return try {
            json.decodeFromString<List<Subscription>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importPomodoroSessions(jsonString: String): List<PomodoroSession> {
        return try {
            json.decodeFromString<List<PomodoroSession>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importSemesters(jsonString: String): List<Semester> {
        return try {
            json.decodeFromString<List<Semester>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importCategories(jsonString: String): List<CategoryBackupDto> {
        return try {
            json.decodeFromString<List<CategoryBackupDto>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun importPreferences(jsonString: String): Map<String, Any> {
        return try {
            json.decodeFromString<Map<String, Any>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
