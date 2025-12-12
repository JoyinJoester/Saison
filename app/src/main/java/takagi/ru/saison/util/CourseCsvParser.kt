package takagi.ru.saison.util

import android.content.Context
import android.net.Uri
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.WeekPattern
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * CSV课程表解析工具
 * 
 * 支持的CSV格式：
 * 课程名称,星期,起始节次,结束节次,地点,教师,开始日期,结束日期
 * 
 * 示例：
 * 高等数学,周一,1,2,A101,张三,2024-09-01,2025-01-15
 * 大学英语,周二,3,4,B202,李四,2024-09-01,2025-01-15
 */
object CourseCsvParser {
    
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * 从CSV文件URI解析课程列表
     */
    fun parseFromUri(context: Context, uri: Uri, semesterId: Long = 1L): List<Course> {
        val courses = mutableListOf<Course>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    // 跳过标题行（如果有）
                    var firstLine = reader.readLine()
                    if (firstLine != null && !firstLine.contains("课程名称")) {
                        // 第一行不是标题，作为数据处理
                        parseLine(firstLine, semesterId)?.let { courses.add(it) }
                    }
                    
                    // 读取所有数据行
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) {
                            parseLine(line, semesterId)?.let { courses.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CourseCsvParser", "Failed to parse CSV", e)
            throw e
        }
        
        android.util.Log.d("CourseCsvParser", "Parsed ${courses.size} courses from CSV")
        return courses
    }
    
    private fun parseLine(line: String, semesterId: Long): Course? {
        try {
            val parts = line.split(",").map { it.trim() }
            
            if (parts.size < 4) {
                android.util.Log.w("CourseCsvParser", "Invalid line: $line")
                return null
            }
            
            val name = parts[0]
            if (name.isBlank()) return null
            
            val dayOfWeek = parseDayOfWeek(parts[1]) ?: return null
            val periodStart = parts[2].toIntOrNull() ?: return null
            val periodEnd = parts[3].toIntOrNull() ?: return null
            
            val location = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
            val instructor = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
            
            val startDate = parts.getOrNull(6)?.let {
                try {
                    LocalDate.parse(it, DATE_FORMATTER)
                } catch (e: Exception) {
                    null
                }
            } ?: LocalDate.now()
            
            val endDate = parts.getOrNull(7)?.let {
                try {
                    LocalDate.parse(it, DATE_FORMATTER)
                } catch (e: Exception) {
                    null
                }
            } ?: startDate.plusMonths(4)
            
            // 生成随机颜色
            val colors = listOf(
                0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD,
                0xFF7986CB, 0xFF64B5F6, 0xFF4FC3F7, 0xFF4DD0E1,
                0xFF4DB6AC, 0xFF81C784, 0xFFAED581, 0xFFDCE775,
                0xFFFFD54F, 0xFFFFB74D, 0xFFFF8A65, 0xFFA1887F
            )
            
            return Course(
                name = name,
                instructor = instructor,
                location = location,
                color = colors.random().toInt(),
                semesterId = semesterId,
                dayOfWeek = dayOfWeek,
                startTime = LocalTime.of(8, 0),  // 占位时间，实际使用节次
                endTime = LocalTime.of(9, 40),
                weekPattern = WeekPattern.ALL,
                startDate = startDate,
                endDate = endDate,
                periodStart = periodStart,
                periodEnd = periodEnd,
                isCustomTime = false
            )
        } catch (e: Exception) {
            android.util.Log.e("CourseCsvParser", "Failed to parse line: $line", e)
            return null
        }
    }
    
    private fun parseDayOfWeek(dayText: String): DayOfWeek? {
        return when {
            dayText.contains("一") || dayText.contains("1") || dayText.lowercase() == "mon" -> DayOfWeek.MONDAY
            dayText.contains("二") || dayText.contains("2") || dayText.lowercase() == "tue" -> DayOfWeek.TUESDAY
            dayText.contains("三") || dayText.contains("3") || dayText.lowercase() == "wed" -> DayOfWeek.WEDNESDAY
            dayText.contains("四") || dayText.contains("4") || dayText.lowercase() == "thu" -> DayOfWeek.THURSDAY
            dayText.contains("五") || dayText.contains("5") || dayText.lowercase() == "fri" -> DayOfWeek.FRIDAY
            dayText.contains("六") || dayText.contains("6") || dayText.lowercase() == "sat" -> DayOfWeek.SATURDAY
            dayText.contains("日") || dayText.contains("天") || dayText.contains("7") || dayText.lowercase() == "sun" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
    
    /**
     * 导出课程到CSV字符串
     */
    fun exportToCsv(courses: List<Course>): String {
        val builder = StringBuilder()
        
        // 添加标题行
        builder.appendLine("课程名称,星期,起始节次,结束节次,地点,教师,开始日期,结束日期")
        
        // 添加数据行
        courses.forEach { course ->
            val dayText = when (course.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }
            
            builder.appendLine(
                listOf(
                    course.name,
                    dayText,
                    course.periodStart ?: "",
                    course.periodEnd ?: "",
                    course.location ?: "",
                    course.instructor ?: "",
                    course.startDate.format(DATE_FORMATTER),
                    course.endDate.format(DATE_FORMATTER)
                ).joinToString(",")
            )
        }
        
        return builder.toString()
    }
}
