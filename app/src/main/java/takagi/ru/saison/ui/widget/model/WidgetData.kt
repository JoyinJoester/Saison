package takagi.ru.saison.ui.widget.model

/**
 * 小组件显示所需的完整数据
 */
data class WidgetData(
    // 顶部信息栏数据
    val semesterName: String,        // 例如："25下" (来自 Semester.name)
    val date: String,                // 例如："11.23"
    val week: String,                // 例如："第13周"
    val dayOfWeek: String,           // 例如："周日"
    
    // 课程数据
    val todayCourses: List<WidgetCourse>,
    val tomorrowCourses: List<WidgetCourse>,
    val hasActiveSemester: Boolean = true
) {
    companion object {
        fun empty() = WidgetData(
            semesterName = "",
            date = "",
            week = "",
            dayOfWeek = "",
            todayCourses = emptyList(),
            tomorrowCourses = emptyList(),
            hasActiveSemester = false
        )
    }
}
