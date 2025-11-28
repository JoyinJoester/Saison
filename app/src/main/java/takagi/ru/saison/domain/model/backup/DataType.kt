package takagi.ru.saison.domain.model.backup

/**
 * 数据类型枚举，定义所有支持的导出导入数据类型
 */
enum class DataType(val fileName: String, val displayNameKey: String) {
    TASKS("tasks.json", "tasks"),
    COURSES("courses.json", "courses"),
    EVENTS("events.json", "events"),
    ROUTINES("routines.json", "routines"),
    SUBSCRIPTIONS("subscriptions.json", "subscriptions"),
    POMODORO_SESSIONS("pomodoro_sessions.json", "pomodoro_sessions"),
    SEMESTERS("semesters.json", "semesters"),
    PREFERENCES("preferences.json", "preferences");
    
    companion object {
        /**
         * 根据文件名查找数据类型
         */
        fun fromFileName(fileName: String): DataType? {
            return values().find { it.fileName == fileName }
        }
    }
}
