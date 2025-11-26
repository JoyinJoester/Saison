package takagi.ru.saison.util

import takagi.ru.saison.domain.model.routine.CycleConfig
import takagi.ru.saison.domain.model.routine.CycleType
import takagi.ru.saison.domain.model.routine.RoutineTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 周期计算工具类
 * 用于计算周期性任务的活跃状态、周期范围和下次活跃日期
 */
class CycleCalculator {
    
    /**
     * 判断任务在指定日期是否处于活跃周期
     * @param task 周期性任务
     * @param date 指定日期，默认为今天
     * @return true 如果任务在活跃周期内
     */
    fun isInActiveCycle(
        task: RoutineTask,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        if (!task.isActive) return false
        
        return when (task.cycleType) {
            CycleType.DAILY -> {
                // 每日任务始终活跃
                true
            }
            CycleType.WEEKLY -> {
                // 每周任务：检查当前星期几是否在配置的日期列表中
                val config = task.cycleConfig as? CycleConfig.Weekly ?: return false
                val currentDayOfWeek = date.dayOfWeek
                config.daysOfWeek.contains(currentDayOfWeek)
            }
            CycleType.MONTHLY -> {
                // 每月任务：检查当前日期是否在配置的日期列表中
                val config = task.cycleConfig as? CycleConfig.Monthly ?: return false
                val currentDayOfMonth = date.dayOfMonth
                config.daysOfMonth.contains(currentDayOfMonth)
            }
            CycleType.CUSTOM -> {
                // 自定义任务：解析简单的 RRULE（FREQ=DAILY;INTERVAL=X）
                val config = task.cycleConfig as? CycleConfig.Custom ?: return false
                val rrule = config.rrule
                
                // 解析间隔天数
                val intervalMatch = Regex("INTERVAL=(\\d+)").find(rrule)
                val interval = intervalMatch?.groupValues?.get(1)?.toIntOrNull() ?: return false
                
                // 计算从任务创建日期到指定日期的天数差
                val daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                    task.createdAt.toLocalDate(),
                    date
                )
                
                // 检查是否是间隔的倍数
                daysSinceCreation >= 0 && daysSinceCreation % interval == 0L
            }
        }
    }
    
    /**
     * 获取任务在指定日期的当前周期范围
     * @param task 周期性任务
     * @param date 指定日期，默认为今天
     * @return 周期范围（开始日期，结束日期），如果不在活跃周期则返回 null
     */
    fun getCurrentCycle(
        task: RoutineTask,
        date: LocalDate = LocalDate.now()
    ): Pair<LocalDate, LocalDate>? {
        if (!task.isActive) return null
        
        return when (task.cycleType) {
            CycleType.DAILY -> {
                // 每日任务：当天 00:00 到 23:59
                Pair(date, date)
            }
            CycleType.WEEKLY -> {
                // 每周任务：当周的周一到周日
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(monday, sunday)
            }
            CycleType.MONTHLY -> {
                // 每月任务：当月 1 日到月末
                val firstDay = date.withDayOfMonth(1)
                val lastDay = date.with(TemporalAdjusters.lastDayOfMonth())
                Pair(firstDay, lastDay)
            }
            CycleType.CUSTOM -> {
                // 自定义任务：根据间隔天数计算周期（当天）
                Pair(date, date)
            }
        }
    }
    
    /**
     * 获取任务的下一个活跃日期
     * @param task 周期性任务
     * @param fromDate 从哪个日期开始查找，默认为今天
     * @return 下一个活跃日期，如果没有则返回 null
     */
    fun getNextActiveDate(
        task: RoutineTask,
        fromDate: LocalDate = LocalDate.now()
    ): LocalDate? {
        if (!task.isActive) return null
        
        // 如果当前日期已经是活跃的，返回当前日期
        if (isInActiveCycle(task, fromDate)) {
            return fromDate
        }
        
        return when (task.cycleType) {
            CycleType.DAILY -> {
                // 每日任务：明天
                fromDate.plusDays(1)
            }
            CycleType.WEEKLY -> {
                // 每周任务：找到下一个配置的星期几
                val config = task.cycleConfig as? CycleConfig.Weekly ?: return null
                if (config.daysOfWeek.isEmpty()) return null
                
                // 从明天开始查找，最多查找 7 天
                for (i in 1..7) {
                    val nextDate = fromDate.plusDays(i.toLong())
                    if (config.daysOfWeek.contains(nextDate.dayOfWeek)) {
                        return nextDate
                    }
                }
                null
            }
            CycleType.MONTHLY -> {
                // 每月任务：找到下一个配置的日期
                val config = task.cycleConfig as? CycleConfig.Monthly ?: return null
                if (config.daysOfMonth.isEmpty()) return null
                
                val currentDayOfMonth = fromDate.dayOfMonth
                val sortedDays = config.daysOfMonth.sorted()
                
                // 先在当月查找
                val nextDayInMonth = sortedDays.firstOrNull { it > currentDayOfMonth }
                if (nextDayInMonth != null) {
                    return try {
                        fromDate.withDayOfMonth(nextDayInMonth)
                    } catch (e: Exception) {
                        // 日期无效（如 2 月 30 日），跳到下个月
                        null
                    }
                }
                
                // 当月没有，查找下个月的第一个日期
                val nextMonth = fromDate.plusMonths(1).withDayOfMonth(1)
                val firstDay = sortedDays.first()
                return try {
                    nextMonth.withDayOfMonth(firstDay)
                } catch (e: Exception) {
                    // 日期无效，继续查找下一个
                    sortedDays.drop(1).firstOrNull()?.let { day ->
                        try {
                            nextMonth.withDayOfMonth(day)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
            CycleType.CUSTOM -> {
                // 自定义任务：根据间隔天数计算下一个活跃日期
                val config = task.cycleConfig as? CycleConfig.Custom ?: return null
                val rrule = config.rrule
                
                // 解析间隔天数
                val intervalMatch = Regex("INTERVAL=(\\d+)").find(rrule)
                val interval = intervalMatch?.groupValues?.get(1)?.toIntOrNull() ?: return null
                
                // 计算从创建日期到当前日期的天数差
                val daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                    task.createdAt.toLocalDate(),
                    fromDate
                )
                
                // 计算下一个活跃日期
                val remainder = daysSinceCreation % interval
                val daysToNext = if (remainder == 0L) 0 else interval - remainder.toInt()
                
                fromDate.plusDays(daysToNext.toLong())
            }
        }
    }
    
    /**
     * 获取周期描述文本
     * @param task 周期性任务
     * @return 周期描述（如"每日"、"每周一、三、五"、"每月 1 日、15 日"）
     */
    fun getCycleDescription(task: RoutineTask): String {
        return when (task.cycleType) {
            CycleType.DAILY -> "每日"
            CycleType.WEEKLY -> {
                val config = task.cycleConfig as? CycleConfig.Weekly
                if (config == null || config.daysOfWeek.isEmpty()) {
                    "每周"
                } else {
                    val dayNames = config.daysOfWeek.sorted().joinToString("、") { day ->
                        when (day) {
                            DayOfWeek.MONDAY -> "周一"
                            DayOfWeek.TUESDAY -> "周二"
                            DayOfWeek.WEDNESDAY -> "周三"
                            DayOfWeek.THURSDAY -> "周四"
                            DayOfWeek.FRIDAY -> "周五"
                            DayOfWeek.SATURDAY -> "周六"
                            DayOfWeek.SUNDAY -> "周日"
                        }
                    }
                    "每$dayNames"
                }
            }
            CycleType.MONTHLY -> {
                val config = task.cycleConfig as? CycleConfig.Monthly
                if (config == null || config.daysOfMonth.isEmpty()) {
                    "每月"
                } else {
                    val days = config.daysOfMonth.sorted().joinToString("、") { "${it}日" }
                    "每月$days"
                }
            }
            CycleType.CUSTOM -> {
                val config = task.cycleConfig as? CycleConfig.Custom
                if (config == null) {
                    "自定义周期"
                } else {
                    // 解析间隔天数
                    val intervalMatch = Regex("INTERVAL=(\\d+)").find(config.rrule)
                    val interval = intervalMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (interval != null) {
                        "每${interval}天"
                    } else {
                        "自定义周期"
                    }
                }
            }
        }
    }
}
