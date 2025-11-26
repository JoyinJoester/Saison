package takagi.ru.saison.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * 订阅统计计算工具类
 * 负责计算订阅的各种统计信息
 */
object SubscriptionStatisticsCalculator {
    
    /**
     * 计算累计时长（人类可读格式）
     * @param startDate 开始日期
     * @param currentDate 当前日期
     * @return 格式化的时长字符串，如 "1年2月15天"
     */
    fun calculateAccumulatedDuration(
        startDate: LocalDate,
        currentDate: LocalDate
    ): String {
        if (currentDate.isBefore(startDate)) {
            return "0天"
        }
        
        val years = ChronoUnit.YEARS.between(startDate, currentDate)
        val months = ChronoUnit.MONTHS.between(startDate.plusYears(years), currentDate)
        val days = ChronoUnit.DAYS.between(
            startDate.plusYears(years).plusMonths(months),
            currentDate
        )
        
        return buildString {
            if (years > 0) append("${years}年")
            if (months > 0) append("${months}月")
            if (days > 0 || (years == 0L && months == 0L)) append("${days}天")
        }
    }
    
    /**
     * 计算累计费用
     * @param startDate 开始日期
     * @param currentDate 当前日期
     * @param price 单价
     * @param cycleType 周期类型
     * @param cycleDuration 周期时长
     * @return 累计费用
     */
    fun calculateAccumulatedCost(
        startDate: LocalDate,
        currentDate: LocalDate,
        price: Double,
        cycleType: String,
        cycleDuration: Int
    ): Double {
        if (currentDate.isBefore(startDate)) {
            return 0.0
        }
        
        val completedCycles = calculateRenewalCyclesCompleted(
            startDate, currentDate, cycleType, cycleDuration
        )
        
        return price * completedCycles
    }
    
    /**
     * 计算平均月费用
     * @param totalCost 总费用
     * @param totalMonths 总月数
     * @return 平均月费用
     */
    fun calculateAverageMonthlyCost(
        totalCost: Double,
        totalMonths: Int
    ): Double {
        if (totalMonths <= 0) return 0.0
        return totalCost / totalMonths
    }
    
    /**
     * 计算平均日费用
     * @param totalCost 总费用
     * @param totalDays 总天数
     * @return 平均日费用
     */
    fun calculateAverageDailyCost(
        totalCost: Double,
        totalDays: Int
    ): Double {
        if (totalDays <= 0) return 0.0
        return totalCost / totalDays
    }
    
    /**
     * 计算已完成的续订周期数
     * @param startDate 开始日期
     * @param currentDate 当前日期
     * @param cycleType 周期类型
     * @param cycleDuration 周期时长
     * @return 完成的周期数
     */
    fun calculateRenewalCyclesCompleted(
        startDate: LocalDate,
        currentDate: LocalDate,
        cycleType: String,
        cycleDuration: Int
    ): Int {
        if (currentDate.isBefore(startDate)) {
            return 0
        }
        
        return when (cycleType) {
            "MONTHLY" -> {
                val totalMonths = ChronoUnit.MONTHS.between(startDate, currentDate)
                max(0, (totalMonths / cycleDuration).toInt())
            }
            "QUARTERLY" -> {
                val totalMonths = ChronoUnit.MONTHS.between(startDate, currentDate)
                val monthsPerCycle = cycleDuration * 3
                max(0, (totalMonths / monthsPerCycle).toInt())
            }
            "YEARLY" -> {
                val totalYears = ChronoUnit.YEARS.between(startDate, currentDate)
                max(0, (totalYears / cycleDuration).toInt())
            }
            "CUSTOM" -> {
                val totalDays = ChronoUnit.DAYS.between(startDate, currentDate)
                max(0, (totalDays / cycleDuration).toInt())
            }
            else -> 0
        }
    }
    
    /**
     * 计算总月数
     * @param startDate 开始日期
     * @param currentDate 当前日期
     * @return 总月数
     */
    fun calculateTotalMonths(
        startDate: LocalDate,
        currentDate: LocalDate
    ): Int {
        if (currentDate.isBefore(startDate)) {
            return 0
        }
        return max(1, ChronoUnit.MONTHS.between(startDate, currentDate).toInt())
    }
    
    /**
     * 计算总天数
     * @param startDate 开始日期
     * @param currentDate 当前日期
     * @return 总天数
     */
    fun calculateTotalDays(
        startDate: LocalDate,
        currentDate: LocalDate
    ): Int {
        if (currentDate.isBefore(startDate)) {
            return 0
        }
        return max(1, ChronoUnit.DAYS.between(startDate, currentDate).toInt())
    }
}
