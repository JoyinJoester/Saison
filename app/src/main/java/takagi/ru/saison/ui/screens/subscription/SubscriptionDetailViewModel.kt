package takagi.ru.saison.ui.screens.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.data.local.database.entities.SubscriptionEntity
import takagi.ru.saison.data.repository.SubscriptionRepository
import takagi.ru.saison.util.RenewalCalculator
import takagi.ru.saison.util.SubscriptionStatisticsCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 订阅详情页面的ViewModel
 */
@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val subscriptionId: Long = savedStateHandle["subscriptionId"] ?: 0L
    
    // 订阅数据
    private val _subscription = MutableStateFlow<SubscriptionEntity?>(null)
    val subscription: StateFlow<SubscriptionEntity?> = _subscription.asStateFlow()
    
    // 统计信息
    private val _statistics = MutableStateFlow<SubscriptionStatistics?>(null)
    val statistics: StateFlow<SubscriptionStatistics?> = _statistics.asStateFlow()
    
    // 续订选项
    private val _renewalOptions = MutableStateFlow<List<RenewalOption>>(emptyList())
    val renewalOptions: StateFlow<List<RenewalOption>> = _renewalOptions.asStateFlow()
    
    // UI状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadSubscription()
    }
    
    /**
     * 加载订阅数据
     */
    private fun loadSubscription() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                val sub = repository.getSubscriptionById(subscriptionId)
                if (sub == null) {
                    _uiState.value = UiState.Error("订阅不存在")
                    return@launch
                }
                
                _subscription.value = sub
                calculateStatistics(sub)
                generateRenewalOptions(sub)
                
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    /**
     * 计算统计信息
     */
    private fun calculateStatistics(subscription: SubscriptionEntity) {
        val startDate = Instant.ofEpochMilli(subscription.startDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val currentDate = LocalDate.now()
        val nextRenewalDate = Instant.ofEpochMilli(subscription.nextRenewalDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        
        val accumulatedDuration = SubscriptionStatisticsCalculator.calculateAccumulatedDuration(
            startDate, currentDate
        )
        
        val accumulatedCost = SubscriptionStatisticsCalculator.calculateAccumulatedCost(
            startDate, currentDate,
            subscription.price,
            subscription.cycleType,
            subscription.cycleDuration
        )
        
        val totalMonths = SubscriptionStatisticsCalculator.calculateTotalMonths(startDate, currentDate)
        val totalDays = SubscriptionStatisticsCalculator.calculateTotalDays(startDate, currentDate)
        
        val averageMonthlyCost = SubscriptionStatisticsCalculator.calculateAverageMonthlyCost(
            accumulatedCost, totalMonths
        )
        
        val averageDailyCost = SubscriptionStatisticsCalculator.calculateAverageDailyCost(
            accumulatedCost, totalDays
        )
        
        val renewalCyclesCompleted = SubscriptionStatisticsCalculator.calculateRenewalCyclesCompleted(
            startDate, currentDate,
            subscription.cycleType,
            subscription.cycleDuration
        )
        
        val daysUntilRenewal = ChronoUnit.DAYS.between(currentDate, nextRenewalDate)
        val isOverdue = daysUntilRenewal < 0
        
        _statistics.value = SubscriptionStatistics(
            accumulatedCost = accumulatedCost,
            accumulatedDuration = accumulatedDuration,
            averageMonthlyCost = averageMonthlyCost,
            averageDailyCost = averageDailyCost,
            renewalCyclesCompleted = renewalCyclesCompleted,
            daysUntilRenewal = daysUntilRenewal,
            isOverdue = isOverdue
        )
    }
    
    /**
     * 生成续订选项
     */
    private fun generateRenewalOptions(subscription: SubscriptionEntity) {
        val currentDate = LocalDate.now()
        val durationOptions = RenewalCalculator.getRenewalDurationOptions(subscription.cycleType)
        
        val options = durationOptions.map { count ->
            val newDate = RenewalCalculator.calculateNextRenewalDate(
                currentDate,
                subscription.cycleType,
                subscription.cycleDuration,
                count
            )
            
            val totalCost = RenewalCalculator.calculateRenewalCost(
                subscription.price,
                count
            )
            
            val label = when (subscription.cycleType) {
                "MONTHLY" -> "${count}个月"
                "QUARTERLY" -> "${count}个季度"
                "YEARLY" -> "${count}年"
                "CUSTOM" -> "${count}个周期"
                else -> "${count}次"
            }
            
            RenewalOption(
                count = count,
                label = label,
                totalCost = totalCost,
                newExpirationDate = newDate
            )
        }
        
        _renewalOptions.value = options
    }
    
    /**
     * 执行手动续订
     */
    fun performManualRenewal(renewalCount: Int) {
        viewModelScope.launch {
            try {
                val sub = _subscription.value ?: return@launch
                
                val currentDate = LocalDate.now()
                val newRenewalDate = RenewalCalculator.calculateNextRenewalDate(
                    currentDate,
                    sub.cycleType,
                    sub.cycleDuration,
                    renewalCount
                )
                
                val newRenewalTimestamp = newRenewalDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                repository.updateRenewalDate(sub.id, newRenewalTimestamp)
                
                // 重新加载数据
                loadSubscription()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("续订失败: ${e.message}")
            }
        }
    }
    
    /**
     * 切换自动续订状态
     */
    fun toggleAutoRenewal() {
        viewModelScope.launch {
            try {
                val sub = _subscription.value ?: return@launch
                val updated = sub.copy(
                    autoRenewal = !sub.autoRenewal,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateSubscription(updated)
                _subscription.value = updated
            } catch (e: Exception) {
                _uiState.value = UiState.Error("更新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 删除订阅
     */
    fun deleteSubscription(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteSubscriptionById(subscriptionId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("删除失败: ${e.message}")
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        loadSubscription()
    }
}

/**
 * 订阅统计信息
 */
data class SubscriptionStatistics(
    val accumulatedCost: Double,
    val accumulatedDuration: String,
    val averageMonthlyCost: Double,
    val averageDailyCost: Double,
    val renewalCyclesCompleted: Int,
    val daysUntilRenewal: Long,
    val isOverdue: Boolean
)

/**
 * 续订选项
 */
data class RenewalOption(
    val count: Int,
    val label: String,
    val totalCost: Double,
    val newExpirationDate: LocalDate
)

/**
 * UI状态
 */
sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}
