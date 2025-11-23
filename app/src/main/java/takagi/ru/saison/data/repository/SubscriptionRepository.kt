package takagi.ru.saison.data.repository

import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.data.local.database.dao.SubscriptionDao
import takagi.ru.saison.data.local.database.entities.SubscriptionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) {
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getActiveSubscriptions()

    suspend fun getSubscriptionById(id: Long): SubscriptionEntity? = subscriptionDao.getSubscriptionById(id)

    suspend fun insertSubscription(subscription: SubscriptionEntity): Long = subscriptionDao.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: SubscriptionEntity) = subscriptionDao.updateSubscription(subscription)

    suspend fun deleteSubscription(subscription: SubscriptionEntity) = subscriptionDao.deleteSubscription(subscription)
    
    suspend fun deleteSubscriptionById(id: Long) = subscriptionDao.deleteSubscriptionById(id)
    
    fun getSubscriptionsRequiringManualRenewal(currentTime: Long): Flow<List<SubscriptionEntity>> = 
        subscriptionDao.getSubscriptionsRequiringManualRenewal(currentTime)
    
    suspend fun updateRenewalDate(id: Long, newDate: Long) = 
        subscriptionDao.updateRenewalDate(id, newDate, System.currentTimeMillis())
}
