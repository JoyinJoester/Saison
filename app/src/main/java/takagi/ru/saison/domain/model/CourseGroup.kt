package takagi.ru.saison.domain.model

import java.time.DayOfWeek

/**
 * 课程组 - 将相同名称的课程合并为一个组
 */
data class CourseGroup(
    val courseName: String,
    val courses: List<Course>,
    val color: Int,
    val instructor: String?,
    val semesterId: Long
) {
    /**
     * 上课次数
     */
    val scheduleCount: Int get() = courses.size
    
    /**
     * 获取所有上课时间的简要描述
     * 例如: "周一1-2节，周三3-4节"
     */
    fun getScheduleSummary(): String {
        return courses.sortedWith(
            compareBy<Course> { it.dayOfWeek.value }
                .thenBy { it.periodStart ?: 0 }
        ).joinToString("，") { course ->
            val day = getDayName(course.dayOfWeek)
            val periods = if (course.isCustomTime) {
                "${course.startTime.hour}:${course.startTime.minute.toString().padStart(2, '0')}-${course.endTime.hour}:${course.endTime.minute.toString().padStart(2, '0')}"
            } else {
                "${course.periodStart}-${course.periodEnd}节"
            }
            val location = if (!course.location.isNullOrBlank()) " ${course.location}" else ""
            "$day$periods$location"
        }
    }
    
    /**
     * 获取星期的中文名称
     */
    private fun getDayName(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    }
}

/**
 * 从课程列表创建课程组列表
 */
fun List<Course>.toCourseGroups(): List<CourseGroup> {
    return this.groupBy { it.name }
        .map { (name, coursesInGroup) ->
            CourseGroup(
                courseName = name,
                courses = coursesInGroup.sortedWith(
                    compareBy<Course> { it.dayOfWeek.value }
                        .thenBy { it.periodStart ?: 0 }
                ),
                color = coursesInGroup.first().color,
                instructor = coursesInGroup.first().instructor,
                semesterId = coursesInGroup.first().semesterId
            )
        }
        .sortedBy { it.courseName }
}
