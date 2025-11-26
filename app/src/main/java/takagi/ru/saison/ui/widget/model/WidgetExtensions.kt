package takagi.ru.saison.ui.widget.model

import takagi.ru.saison.domain.model.Course
import java.time.format.DateTimeFormatter

/**
 * 将 Course 转换为 WidgetCourse
 */
fun Course.toWidgetCourse(): WidgetCourse? {
    return try {
        WidgetCourse(
            id = id,
            name = name,
            location = location,
            startTime = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            endTime = endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = color
        )
    } catch (e: Exception) {
        // 如果转换失败，返回 null
        null
    }
}
