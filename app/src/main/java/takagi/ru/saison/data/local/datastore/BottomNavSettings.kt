package takagi.ru.saison.data.local.datastore

/**
 * 底部导航标签枚举
 */
enum class BottomNavTab {
    COURSE,
    CALENDAR,
    TASKS,
    POMODORO,
    SUBSCRIPTION,
    SETTINGS;
    
    companion object {
        val DEFAULT_ORDER = listOf(
            CALENDAR,
            TASKS,
            COURSE,
            POMODORO,
            SUBSCRIPTION,
            SETTINGS
        )
        
        /**
         * 从字符串解析导航顺序
         */
        fun parseOrder(orderString: String): List<BottomNavTab> {
            if (orderString.isBlank()) return DEFAULT_ORDER
            
            return try {
                orderString.split(",")
                    .mapNotNull { name ->
                        try {
                            valueOf(name.trim())
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }
                    .let { parsed ->
                        // 确保所有标签都存在
                        val result = parsed.toMutableList()
                        values().forEach { tab ->
                            if (tab !in result) {
                                result.add(tab)
                            }
                        }
                        result
                    }
            } catch (e: Exception) {
                DEFAULT_ORDER
            }
        }
        
        /**
         * 将导航顺序转换为字符串
         */
        fun orderToString(order: List<BottomNavTab>): String {
            return order.joinToString(",") { it.name }
        }
    }
}

/**
 * 底部导航可见性设置
 */
data class BottomNavVisibility(
    val course: Boolean = true,
    val tasks: Boolean = true,
    val pomodoro: Boolean = true,
    val subscription: Boolean = true,
    val settings: Boolean = true  // 设置项始终可见
) {
    /**
     * 检查指定标签是否可见
     */
    fun isVisible(tab: BottomNavTab): Boolean = when (tab) {
        BottomNavTab.COURSE -> course
        BottomNavTab.CALENDAR -> true  // Calendar is always visible
        BottomNavTab.TASKS -> tasks
        BottomNavTab.POMODORO -> pomodoro
        BottomNavTab.SUBSCRIPTION -> subscription
        BottomNavTab.SETTINGS -> settings
    }
    
    /**
     * 计算可见项数量
     */
    fun visibleCount(): Int = listOf(
        course, tasks, pomodoro, subscription, settings
    ).count { it }
    
    /**
     * 更新指定标签的可见性
     */
    fun withVisibility(tab: BottomNavTab, visible: Boolean): BottomNavVisibility {
        return when (tab) {
            BottomNavTab.COURSE -> copy(course = visible)
            BottomNavTab.CALENDAR -> this  // Calendar visibility cannot be changed
            BottomNavTab.TASKS -> copy(tasks = visible)
            BottomNavTab.POMODORO -> copy(pomodoro = visible)
            BottomNavTab.SUBSCRIPTION -> copy(subscription = visible)
            BottomNavTab.SETTINGS -> copy(settings = visible)
        }
    }
}
