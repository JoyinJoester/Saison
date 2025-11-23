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
    private val repository: SubscriptionRepository
) : ViewModel() {

    val subscriptions = repository.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        note: String?,
        autoRenewal: Boolean,
        reminderEnabled: Boolean,
        reminderDaysBefore: Int
    ) {
        viewModelScope.launch {
            val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val nextRenewal = calculateNextRenewalDate(startDate, cycleType, cycleDuration)
            val nextRenewalTimestamp = nextRenewal.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (id != null) {
                // Update existing subscription
                // Get the original subscription to preserve createdAt
                val originalSubscription = subscriptions.value.find { it.id == id }
                val entity = SubscriptionEntity(
                    id = id,
                    name = name,
                    category = category,
                    price = price,
                    cycleType = cycleType,
                    cycleDuration = cycleDuration,
                    startDate = startTimestamp,
                    nextRenewalDate = nextRenewalTimestamp,
                    autoRenewal = autoRenewal,
                    reminderEnabled = reminderEnabled,
                    reminderDaysBefore = reminderDaysBefore,
                    note = note,
                    isActive = true,
                    createdAt = originalSubscription?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
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
                    nextRenewalDate = nextRenewalTimestamp,
                    autoRenewal = autoRenewal,
                    reminderEnabled = reminderEnabled,
                    reminderDaysBefore = reminderDaysBefore,
                    note = note,
                    isActive = true
                )
                repository.insertSubscription(entity)
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
}

data class SubscriptionStats(
    val accumulatedDuration: String,
    val accumulatedCost: Double,
    val status: SubscriptionStatus,
    val averageMonthlyCost: Double,
    val averageDailyCost: Double
)
