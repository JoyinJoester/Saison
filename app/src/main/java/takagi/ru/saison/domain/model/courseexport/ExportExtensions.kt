package takagi.ru.saison.domain.model.courseexport

import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.CourseSettings
import takagi.ru.saison.domain.model.Semester
import takagi.ru.saison.domain.model.WeekPattern
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Domain Model 到 Export Data 的转换扩展函数
 */

// Semester -> SemesterInfo
fun Semester.toSemesterInfo(currentWeek: Int): SemesterInfo {
    return SemesterInfo(
        name = this.name,
        startDate = this.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        endDate = this.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
        currentWeek = currentWeek,
        totalWeeks = this.totalWeeks
    )
}

// CourseSettings -> PeriodSettingsData
fun CourseSettings.toPeriodSettingsData(): PeriodSettingsData {
    return PeriodSettingsData(
        totalPeriods = this.totalPeriods,
        periodDurationMinutes = this.periodDuration,
        breakDurationMinutes = this.breakDuration,
        firstPeriodStartTime = this.firstPeriodStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        lunchBreakAfterPeriod = this.lunchBreakAfterPeriod,
        lunchBreakDurationMinutes = this.lunchBreakDuration
    )
}

// CourseSettings -> DisplaySettingsData
fun CourseSettings.toDisplaySettingsData(): DisplaySettingsData {
    return DisplaySettingsData(
        showWeekend = this.showWeekends,
        timeFormat24Hour = true, // 默认使用24小时制
        showPeriodNumber = true,  // 默认显示节次号
        compactMode = false       // 默认非紧凑模式
    )
}

// Course -> CourseData
fun Course.toCourseData(): CourseData {
    return CourseData(
        name = this.name,
        teacher = this.instructor,
        location = this.location,
        dayOfWeek = this.dayOfWeek.value,
        startPeriod = this.periodStart ?: 1,
        endPeriod = this.periodEnd ?: 1,
        startTime = this.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        endTime = this.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        weekPattern = this.weekPattern.toWeekPatternData(this.customWeeks),
        color = String.format("#%08X", this.color),
        notes = null // 当前Course模型没有notes字段
    )
}

// WeekPattern -> WeekPatternData
fun WeekPattern.toWeekPatternData(customWeeks: List<Int>?): WeekPatternData {
    return WeekPatternData(
        type = this.name,
        customWeeks = if (this == WeekPattern.CUSTOM) customWeeks else null
    )
}


/**
 * Export Data 到 Domain Model 的转换扩展函数
 */

// SemesterInfo -> Semester
fun SemesterInfo.toSemester(): Semester {
    return Semester(
        id = 0, // 新创建的学期，ID由数据库生成
        name = this.name,
        startDate = LocalDate.parse(this.startDate, DateTimeFormatter.ISO_LOCAL_DATE),
        endDate = LocalDate.parse(this.endDate, DateTimeFormatter.ISO_LOCAL_DATE),
        totalWeeks = this.totalWeeks,
        isArchived = false,
        isDefault = false
    )
}

// CourseData -> Course
fun CourseData.toCourse(semesterId: Long, semesterStartDate: LocalDate, semesterEndDate: LocalDate): Course {
    val dayOfWeek = DayOfWeek.of(this.dayOfWeek)
    val startTime = LocalTime.parse(this.startTime, DateTimeFormatter.ofPattern("HH:mm"))
    val endTime = LocalTime.parse(this.endTime, DateTimeFormatter.ofPattern("HH:mm"))
    val weekPattern = WeekPattern.fromString(this.weekPattern.type)
    
    // 解析颜色（支持 #AARRGGBB 和 #RRGGBB 格式）
    val colorInt = try {
        android.graphics.Color.parseColor(this.color)
    } catch (e: Exception) {
        0xFF6200EE.toInt() // 默认颜色
    }
    
    return Course(
        id = 0, // 新创建的课程，ID由数据库生成
        name = this.name,
        instructor = this.teacher,
        location = this.location,
        color = colorInt,
        semesterId = semesterId,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        weekPattern = weekPattern,
        customWeeks = this.weekPattern.customWeeks,
        startDate = semesterStartDate,
        endDate = semesterEndDate,
        periodStart = this.startPeriod,
        periodEnd = this.endPeriod,
        isCustomTime = false
    )
}

// PeriodSettingsData -> CourseSettings 应用方法
fun PeriodSettingsData.applyCourseSettings(currentSettings: CourseSettings): CourseSettings {
    return currentSettings.copy(
        totalPeriods = this.totalPeriods,
        periodDuration = this.periodDurationMinutes,
        breakDuration = this.breakDurationMinutes,
        firstPeriodStartTime = LocalTime.parse(this.firstPeriodStartTime, DateTimeFormatter.ofPattern("HH:mm")),
        lunchBreakAfterPeriod = this.lunchBreakAfterPeriod,
        lunchBreakDuration = this.lunchBreakDurationMinutes ?: 90
    )
}

// DisplaySettingsData -> CourseSettings 应用方法
fun DisplaySettingsData.applyCourseSettings(currentSettings: CourseSettings): CourseSettings {
    return currentSettings.copy(
        showWeekends = this.showWeekend
    )
}
