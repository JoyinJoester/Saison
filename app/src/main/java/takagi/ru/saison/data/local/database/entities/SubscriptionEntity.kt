package takagi.ru.saison.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // e.g., "数字产品", "生活服务"
    val price: Double,
    val currency: String = "CNY",
    val cycleType: String, // "MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM"
    val cycleDuration: Int = 1,
    val startDate: Long, // Timestamp
    val nextRenewalDate: Long, // Timestamp
    val autoRenewal: Boolean = true, // NEW: Auto-renewal enabled by default
    val reminderEnabled: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
