package takagi.ru.saison.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 续订计算工具类
 * 负责计算订阅的续订日期和费用
 */
object RenewalCalculator {
    
    /**
     * 计算下一个续订日期
     * @param currentDate 当前日期
     * @param cycleType 周期类型 ("MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM")
     * @param cycleDuration 周期时长
     * @param renewalCount 续订次数（续订几个周期）
     * @return 新的续订日期
     */
    fun calculateNextRenewalDate(
        currentDate: LocalDate,
        cycleType: String,
        cycleDuration: Int,
        renewalCount: Int
    ): LocalDate {
        return when (cycleType) {
            "MONTHLY" -> {
                // 添加月份，处理月末日期
                var result = currentDate
                repeat(renewalCount) {
                    result = addMonthsSafely(result, cycleDuration)
                }
                result
            }
            "QUARTERLY" -> {
                // 季度 = 3个月
                var result = currentDate
                repeat(renewalCount) {
                    result = addMonthsSafely(result, cycleDuration * 3)
                }
                result
            }
            "YEARLY" -> {
                // 添加年份
                currentDate.plusYears((cycleDuration * renewalCount).toLong())
            }
            "CUSTOM" -> {
                // 自定义周期，按天计算
                currentDate.plusDays((cycleDuration * renewalCount).toLong())
            }
            else -> currentDate
        }
    }
    
    /**
     * 安全地添加月份，处理月末日期
     * 例如：1月31日 + 1个月 = 2月28日（或29日）
     */
    private fun addMonthsSafely(date: LocalDate, months: Int): LocalDate {
        val targetMonth = date.plusMonths(months.toLong())
        // 如果原日期是月末，确保结果也是月末
        val lastDayOfTargetMonth = targetMonth.lengthOfMonth()
        return if (date.dayOfMonth > lastDayOfTargetMonth) {
            targetMonth.withDayOfMonth(lastDayOfTargetMonth)
        } else {
            targetMonth
        }
    }
    
    /**
     * 计算续订费用
     * @param basePrice 基础价格
     * @param renewalCount 续订次数
     * @return 总费用
     */
    fun calculateRenewalCost(
        basePrice: Double,
        renewalCount: Int
    ): Double {
        return basePrice * renewalCount
    }
    
    /**
     * 获取续订时长选项
     * @param cycleType 周期类型
     * @return 可选的续订次数列表
     */
    fun getRenewalDurationOptions(cycleType: String): List<Int> {
        return when (cycleType) {
            "MONTHLY" -> listOf(1, 3, 6, 12) // 1个月、3个月、半年、1年
            "QUARTERLY" -> listOf(1, 2, 4) // 1季度、半年、1年
            "YEARLY" -> listOf(1, 2, 3) // 1年、2年、3年
            "CUSTOM" -> listOf(1, 2, 3, 5, 10) // 自定义周期的倍数
            else -> listOf(1)
        }
    }
}
