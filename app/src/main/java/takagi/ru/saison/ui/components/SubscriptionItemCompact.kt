package takagi.ru.saison.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.data.local.database.entities.SubscriptionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun SubscriptionItemCompact(
    subscription: SubscriptionEntity,
    selectedDate: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val renewalDate = Instant.ofEpochMilli(subscription.nextRenewalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    
    val daysUntilRenewal = ChronoUnit.DAYS.between(selectedDate, renewalDate)
    val isUrgent = daysUntilRenewal <= 1
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isUrgent) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isUrgent) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Days until renewal
                val daysText = when {
                    daysUntilRenewal == 0L -> "今天续费"
                    daysUntilRenewal == 1L -> "明天续费"
                    daysUntilRenewal < 0L -> "已过期"
                    else -> "${daysUntilRenewal}天后续费"
                }
                
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUrgent) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            // Price
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${subscription.currency} ${String.format("%.2f", subscription.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (subscription.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subscription.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
