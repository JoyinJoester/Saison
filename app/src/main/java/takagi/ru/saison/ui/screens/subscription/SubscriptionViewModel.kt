package takagi.ru.saison.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.data.local.database.entities.SubscriptionEntity
import takagi.ru.saison.data.repository.SubscriptionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val historyManager: takagi.ru.saison.util.SubscriptionHistoryManager,
    private val categoryRepository: takagi.ru.saison.data.repository.CategoryRepository
) : ViewModel() {

    val subscriptions = repository.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 分类列表
    val categories = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 筛选模式
    private val _filterMode = MutableStateFlow(SubscriptionFilterMode.ALL)
    val filterMode: StateFlow<SubscriptionFilterMode> = _filterMode.asStateFlow()
    
    // 选中的分类（用于筛选）
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    // 上次选择的添加分类（用于记住用户选择）
    private val _lastSelectedAddCategory = MutableStateFlow("默认")
    val lastSelectedAddCategory: StateFlow<String> = _lastSelectedAddCategory.asStateFlow()
    
    // 筛选后的订阅列表
    val filteredSubscriptions = combine(subscriptions, _filterMode, _searchQuery, _selectedCategory) { subs, mode, query, category ->
        var filtered = when (mode) {
            SubscriptionFilterMode.ALL -> subs
            SubscriptionFilterMode.ACTIVE -> subs.filter { it.isActive && !it.isPaused }
            SubscriptionFilterMode.OVERDUE -> subs.filter { 
                val nextRenewal = Instant.ofEpochMilli(it.nextRenewalDate).atZone(ZoneId.systemDefault()).toLocalDate()
                nextRenewal.isBefore(LocalDate.now())
            }
            SubscriptionFilterMode.PAUSED -> subs.filter { it.isPaused }
        }
        
        // 按分类筛选
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }
        
        // 按搜索查询筛选
        if (query.isNotBlank()) {
            filtered = filtered.filter { subscription ->
                subscription.name.contains(query, ignoreCase = true) ||
                subscription.category.contains(query, ignoreCase = true) ||
                subscription.note?.contains(query, ignoreCase = true) == true
            }
        }
        
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 统计数据
    val statistics = subscriptions.map { subs ->
        calculateGlobalStatistics(subs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionGlobalStats())
    
    fun setFilterMode(mode: SubscriptionFilterMode) {
        _filterMode.value = mode
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }
    
    fun setLastSelectedAddCategory(category: String) {
        _lastSelectedAddCategory.value = category
    }
    
    fun addCategory(name: String) {
        viewModelScope.launch {
            val category = takagi.ru.saison.data.local.database.entities.CategoryEntity(
                name = name,
                isDefault = false
            )
            categoryRepository.insertCategory(category)
        }
    }
    
    fun renameCategory(oldCategory: takagi.ru.saison.data.local.database.entities.CategoryEntity, newName: String) {
        viewModelScope.launch {
            // 1. 更新分类名称
            val updatedCategory = oldCategory.copy(name = newName, updatedAt = System.currentTimeMillis())
            categoryRepository.updateCategory(updatedCategory)
            
            // 2. 更新该分类下所有订阅的分类名称
            val subsToUpdate = subscriptions.value.filter { it.category == oldCategory.name }
            subsToUpdate.forEach { sub ->
                val updatedSub = sub.copy(category = newName, updatedAt = System.currentTimeMillis())
                repository.updateSubscription(updatedSub)
            }
        }
    }
    
    fun deleteCategory(category: takagi.ru.saison.data.local.database.entities.CategoryEntity) {
        viewModelScope.launch {
            // 1. 将该分类下的所有订阅移动到"默认"分类
            val subsToUpdate = subscriptions.value.filter { it.category == category.name }
            subsToUpdate.forEach { sub ->
                val updatedSub = sub.copy(category = "默认", updatedAt = System.currentTimeMillis())
                repository.updateSubscription(updatedSub)
            }
            
            // 2. 删除分类
            categoryRepository.deleteCategory(category)
        }
    }

    fun addSubscription(
        name: String,
        category: String,
        price: Double,
        cycleType: String,
        cycleDuration: Int,
        startDate: LocalDate,
        note: String?
    ) {
        viewModelScope.launch {
            val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val nextRenewal = calculateNextRenewalDate(startDate, cycleType, cycleDuration)
            val nextRenewalTimestamp = nextRenewal.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val entity = SubscriptionEntity(
                name = name,
                category = category,
                price = price,
                cycleType = cycleType,
                cycleDuration = cycleDuration,
                startDate = startTimestamp,
                nextRenewalDate = nextRenewalTimestamp,
                note = note,
                isActive = true
            )
            repository.insertSubscription(entity)
        }
    }
    
    fun saveSubscription(
        id: Long?,
        name: String,
        category: String,
        price: Double,
        cycleType: String,
        cycleDuration: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        note: String?,
        autoRenewal: Boolean,
        reminderEnabled: Boolean,
        reminderDaysBefore: Int
    ) {
        viewModelScope.launch {
            val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = endDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            
            // 如果设置了结束日期，使用结束日期作为下次续订日期
            val nextRenewal = endDate ?: calculateNextRenewalDate(startDate, cycleType, cycleDuration)
            val nextRenewalTimestamp = nextRenewal.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (id != null) {
                // Update existing subscription
                val originalSubscription = subscriptions.value.find { it.id == id }
                val entity = SubscriptionEntity(
                    id = id,
                    name = name,
                    category = category,
                    price = price,
                    cycleType = cycleType,
                    cycleDuration = cycleDuration,
                    startDate = startTimestamp,
                    endDate = endTimestamp,
                    nextRenewalDate = nextRenewalTimestamp,
                    autoRenewal = autoRenewal,
                    reminderEnabled = reminderEnabled,
                    reminderDaysBefore = reminderDaysBefore,
                    note = note,
                    isActive = true,
                    isPaused = originalSubscription?.isPaused ?: false,
                    createdAt = originalSubscription?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                if (originalSubscription != null) {
                    historyManager.recordModified(originalSubscription, entity)
                }
                repository.updateSubscription(entity)
            } else {
                // Create new subscription
                val entity = SubscriptionEntity(
                    name = name,
                    category = category,
                    price = price,
                    cycleType = cycleType,
                    cycleDuration = cycleDuration,
                    startDate = startTimestamp,
                    endDate = endTimestamp,
                    nextRenewalDate = nextRenewalTimestamp,
                    autoRenewal = autoRenewal,
                    reminderEnabled = reminderEnabled,
                    reminderDaysBefore = reminderDaysBefore,
                    note = note,
                    isActive = true
                )
                val newId = repository.insertSubscription(entity)
                
                // 记录创建历史
                val createdEntity = entity.copy(id = newId)
                historyManager.recordCreated(createdEntity)
            }
        }
    }
    
    fun updateSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch {
            repository.deleteSubscriptionById(id)
        }
    }

    private fun calculateNextRenewalDate(startDate: LocalDate, cycleType: String, cycleDuration: Int): LocalDate {
        val today = LocalDate.now()
        var nextDate = startDate
        
        // 如果开始日期在未来，则下次续费日期就是开始日期
        if (nextDate.isAfter(today)) {
            return nextDate
        }

        while (!nextDate.isAfter(today)) {
            nextDate = when (cycleType) {
                "MONTHLY" -> nextDate.plusMonths(cycleDuration.toLong())
                "QUARTERLY" -> nextDate.plusMonths(cycleDuration.toLong() * 3)
                "YEARLY" -> nextDate.plusYears(cycleDuration.toLong())
                else -> nextDate.plusMonths(1) // Default fallback
            }
        }
        return nextDate
    }
    
    // Helper to calculate accumulated cost and duration for UI
    fun calculateStats(subscription: SubscriptionEntity): SubscriptionStats {
        val startDate = Instant.ofEpochMilli(subscription.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        
        if (startDate.isAfter(today)) {
            return SubscriptionStats("0天", 0.0, SubscriptionStatus.Active, 0.0, 0.0)
        }

        val durationDays = ChronoUnit.DAYS.between(startDate, today)
        val durationString = formatDuration(startDate, today)
        
        // Calculate cost
        // This is an approximation. For precise billing history, we'd need a transaction log.
        var cost = 0.0
        var tempDate = startDate
        while (tempDate.isBefore(today) || tempDate.isEqual(today)) {
            cost += subscription.price
             tempDate = when (subscription.cycleType) {
                "MONTHLY" -> tempDate.plusMonths(subscription.cycleDuration.toLong())
                "QUARTERLY" -> tempDate.plusMonths(subscription.cycleDuration.toLong() * 3)
                "YEARLY" -> tempDate.plusYears(subscription.cycleDuration.toLong())
                else -> tempDate.plusMonths(1)
            }
        }
        
        // Check status
        val nextRenewal = Instant.ofEpochMilli(subscription.nextRenewalDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val status = if (nextRenewal.isBefore(today)) {
            SubscriptionStatus.Overdue(ChronoUnit.DAYS.between(nextRenewal, today))
        } else {
            SubscriptionStatus.Active
        }

        // Calculate average costs
        val monthlyCost = when (subscription.cycleType) {
            "MONTHLY" -> subscription.price / subscription.cycleDuration
            "QUARTERLY" -> subscription.price / (subscription.cycleDuration * 3)
            "YEARLY" -> subscription.price / (subscription.cycleDuration * 12)
            else -> 0.0
        }
        
        val dailyCost = when (subscription.cycleType) {
            "MONTHLY" -> subscription.price / (subscription.cycleDuration * 30.44)
            "QUARTERLY" -> subscription.price / (subscription.cycleDuration * 3 * 30.44)
            "YEARLY" -> subscription.price / (subscription.cycleDuration * 365)
            else -> 0.0
        }

        return SubscriptionStats(durationString, cost, status, monthlyCost, dailyCost)
    }

    private fun formatDuration(start: LocalDate, end: LocalDate): String {
        val period = java.time.Period.between(start, end)
        val years = period.years
        val months = period.months
        val days = period.days
        
        val parts = mutableListOf<String>()
        if (years > 0) parts.add("${years}年")
        if (months > 0) parts.add("${months}月")
        if (days > 0) parts.add("${days}天")
        
        return if (parts.isEmpty()) "0天" else parts.joinToString("")
    }
    
    // 计算全局统计数据
    private fun calculateGlobalStatistics(subscriptions: List<SubscriptionEntity>): SubscriptionGlobalStats {
        if (subscriptions.isEmpty()) {
            return SubscriptionGlobalStats()
        }
        
        val today = LocalDate.now()
        var totalDailyCost = 0.0
        var totalMonthlyCost = 0.0
        var activeCount = 0
        var overdueCount = 0
        var pausedCount = 0
        
        // 一次性购买统计
        var oneTimePurchaseValue = 0.0
        var oneTimePurchaseDailyValue = 0.0
        var totalCost = 0.0
        
        subscriptions.forEach { sub ->
            val stats = calculateStats(sub)
            
            // 统计状态
            if (sub.isActive && !sub.isPaused) {
                activeCount++
            }
            if (sub.isPaused) {
                pausedCount++
            }
            if (stats.status is SubscriptionStatus.Overdue) {
                overdueCount++
            }
            
            // 周期性订阅的成本
            if (sub.cycleType != "ONE_TIME") {
                totalDailyCost += stats.averageDailyCost
                totalMonthlyCost += stats.averageMonthlyCost
                totalCost += stats.accumulatedCost
            } else {
                // 一次性购买:计算从购买日到今天的日均使用价值
                val startDate = Instant.ofEpochMilli(sub.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
                val daysSincePurchase = ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(1)
                val dailyValue = sub.price / daysSincePurchase
                
                oneTimePurchaseValue += sub.price
                oneTimePurchaseDailyValue += dailyValue
                totalCost += sub.price
            }
        }
        
        return SubscriptionGlobalStats(
            totalDailyCost = totalDailyCost,
            totalMonthlyCost = totalMonthlyCost,
            activeCount = activeCount,
            overdueCount = overdueCount,
            pausedCount = pausedCount,
            oneTimePurchaseTotalValue = oneTimePurchaseValue,
            oneTimePurchaseDailyValue = oneTimePurchaseDailyValue,
            totalSubscriptions = subscriptions.size,
            totalCost = totalCost
        )
    }
}

data class SubscriptionStats(
    val accumulatedDuration: String,
    val accumulatedCost: Double,
    val status: SubscriptionStatus,
    val averageMonthlyCost: Double,
    val averageDailyCost: Double
)

data class SubscriptionGlobalStats(
    val totalDailyCost: Double = 0.0,
    val totalMonthlyCost: Double = 0.0,
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    val pausedCount: Int = 0,
    val oneTimePurchaseTotalValue: Double = 0.0,
    val oneTimePurchaseDailyValue: Double = 0.0,
    val totalSubscriptions: Int = 0,
    val totalCost: Double = 0.0
)

enum class SubscriptionFilterMode {
    ALL,      // 全部
    ACTIVE,   // 进行中
    OVERDUE,  // 已逾期
    PAUSED    // 已暂停
}
