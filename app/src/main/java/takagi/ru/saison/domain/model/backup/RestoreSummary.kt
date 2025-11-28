package takagi.ru.saison.domain.model.backup

/**
 * 恢复操作摘要
 */
data class RestoreSummary(
    val importedTasks: Int = 0,
    val importedCourses: Int = 0,
    val importedEvents: Int = 0,
    val importedRoutines: Int = 0,
    val importedSubscriptions: Int = 0,
    val importedPomodoroSessions: Int = 0,
    val importedSemesters: Int = 0,
    val skippedDuplicates: Int = 0
) {
    val totalImported: Int
        get() = importedTasks + importedCourses + importedEvents + 
                importedRoutines + importedSubscriptions + 
                importedPomodoroSessions + importedSemesters
}
