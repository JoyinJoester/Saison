package takagi.ru.saison.domain.model.courseexport

import kotlinx.serialization.Serializable

/**
 * 课程表导出数据的根对象
 * 包含元数据和一个或多个学期的完整配置
 */
@Serializable
data class CourseExportData(
    val metadata: ExportMetadata,
    val semesters: List<SemesterExportData>
)

/**
 * 导出元数据
 * 包含版本信息、导出时间等
 */
@Serializable
data class ExportMetadata(
    val version: String = "1.0",
    val exportTime: Long,
    val appVersion: String,
    val deviceInfo: String
)

/**
 * 单个学期的导出数据
 * 包含学期信息、节次设置、显示设置和所有课程
 */
@Serializable
data class SemesterExportData(
    val semesterInfo: SemesterInfo,
    val periodSettings: PeriodSettingsData,
    val displaySettings: DisplaySettingsData,
    val courses: List<CourseData>
)

/**
 * 学期信息
 * 包含学期的基本信息和周数配置
 */
@Serializable
data class SemesterInfo(
    val name: String,
    val startDate: String, // ISO 8601 format (yyyy-MM-dd)
    val endDate: String,   // ISO 8601 format (yyyy-MM-dd)
    val currentWeek: Int,
    val totalWeeks: Int
)

/**
 * 节次设置数据
 * 包含课程表的时间配置
 */
@Serializable
data class PeriodSettingsData(
    val totalPeriods: Int,
    val periodDurationMinutes: Int,
    val breakDurationMinutes: Int,
    val firstPeriodStartTime: String, // HH:mm format
    val lunchBreakAfterPeriod: Int?,
    val lunchBreakDurationMinutes: Int?
)

/**
 * 显示设置数据
 * 包含课程表的显示偏好
 */
@Serializable
data class DisplaySettingsData(
    val showWeekend: Boolean,
    val timeFormat24Hour: Boolean,
    val showPeriodNumber: Boolean,
    val compactMode: Boolean
)

/**
 * 课程数据
 * 包含单个课程的完整信息
 */
@Serializable
data class CourseData(
    val name: String,
    val teacher: String?,
    val location: String?,
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val startPeriod: Int,
    val endPeriod: Int,
    val startTime: String, // HH:mm format
    val endTime: String,   // HH:mm format
    val weekPattern: WeekPatternData,
    val color: String, // Hex color format (#AARRGGBB)
    val notes: String?
)

/**
 * 周数模式数据
 * 定义课程在哪些周上课
 */
@Serializable
data class WeekPatternData(
    val type: String, // "ALL", "A", "B", "ODD", "EVEN", "CUSTOM"
    val customWeeks: List<Int>? = null // For CUSTOM type only
)
