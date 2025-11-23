package takagi.ru.saison.data.local.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import takagi.ru.saison.data.local.database.entities.SubscriptionEntity

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextRenewalDate ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextRenewalDate ASC")
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)
    
    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)
    
    @Query("SELECT * FROM subscriptions WHERE autoRenewal = 0 AND nextRenewalDate < :currentTime AND isActive = 1 ORDER BY nextRenewalDate ASC")
    fun getSubscriptionsRequiringManualRenewal(currentTime: Long): Flow<List<SubscriptionEntity>>
    
    @Query("UPDATE subscriptions SET nextRenewalDate = :newDate, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateRenewalDate(id: Long, newDate: Long, updateTime: Long)
}
