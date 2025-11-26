package takagi.ru.saison.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * 课程设置数据模型
 * 存储课程表的全局配置信息
 */
data class CourseSettings(
    val totalPeriods: Int = 8,                                       // 每天总节次数
    val periodDuration: Int = 45,                                    // 课程时长（分钟）
    val breakDuration: Int = 10,                                     // 课间休息（分钟）
    val firstPeriodStartTime: LocalTime = LocalTime.of(8, 0),       // 第一节课开始时间
    val lunchBreakAfterPeriod: Int? = 4,                            // 午休在第几节课后
    val lunchBreakDuration: Int = 90,                               // 午休时长（分钟）
    val dinnerBreakDuration: Int = 60,                              // 晚休时长（分钟）（暂时保留但不使用）
    @Deprecated("Use Semester.startDate from SemesterRepository instead. This field is kept for backward compatibility only.")
    val semesterStartDate: LocalDate? = null,                        // 学期第一周开始日期（已弃用，请使用 Semester.startDate）
    val totalWeeks: Int = 18,                                        // 学期总周数
    val gridCellHeight: Int = 80,                                   // 网格单元格高度（dp）
    val showWeekends: Boolean = true,                               // 是否显示周末
    val autoScrollToCurrentTime: Boolean = true,                    // 是否自动滚动到当前时间
    val highlightCurrentPeriod: Boolean = true,                     // 是否高亮当前节次
    val breakPeriods: List<BreakPeriod> = emptyList(),              // 休息时段配置（将由生成器自动填充）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 保留这些字段用于向后兼容，但标记为已弃用
    @Deprecated("Use totalPeriods instead", ReplaceWith("totalPeriods"))
    val periodsPerDay: Int get() = totalPeriods
    
    @Deprecated("No longer used - periods are not divided by time of day", ReplaceWith("totalPeriods"))
    val morningPeriods: Int get() = 0
    
    @Deprecated("No longer used - periods are not divided by time of day", ReplaceWith("totalPeriods"))
    val afternoonPeriods: Int get() = 0
    
    @Deprecated("No longer used - periods are not divided by time of day", ReplaceWith("totalPeriods"))
    val eveningPeriods: Int get() = 0
}
