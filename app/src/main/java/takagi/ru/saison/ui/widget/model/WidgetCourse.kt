package takagi.ru.saison.ui.widget.model

/**
 * 单个课程在小组件中的显示数据
 */
data class WidgetCourse(
    val id: Long,
    val name: String,
    val location: String?,
    val startTime: String,           // "10:00"
    val endTime: String,             // "11:40"
    val color: Int,                  // Color value
    val isCurrent: Boolean = false   // 是否是当前正在上的课程
)
