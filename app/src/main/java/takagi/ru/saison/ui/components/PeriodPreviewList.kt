package takagi.ru.saison.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import takagi.ru.saison.domain.model.CoursePeriod
import java.time.format.DateTimeFormatter

/**
 * 节次时间预览列表组件
 */
@Composable
fun PeriodPreviewList(
    periods: List<CoursePeriod>,
    onPeriodClick: ((CoursePeriod) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(periods) { period ->
            PeriodPreviewItem(
                period = period,
                onClick = onPeriodClick?.let { { it(period) } }
            )
            
            // 检查是否需要显示休息时段标识
            val currentIndex = periods.indexOf(period)
            if (currentIndex < periods.size - 1) {
                val nextPeriod = periods[currentIndex + 1]
                // 如果下一节课的时段不同，显示休息标识
                if (period.timeOfDay != nextPeriod.timeOfDay) {
                    when (nextPeriod.timeOfDay) {
                        takagi.ru.saison.domain.model.TimeOfDay.AFTERNOON -> LunchBreakIndicator()
                        takagi.ru.saison.domain.model.TimeOfDay.EVENING -> DinnerBreakIndicator()
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodPreviewItem(
    period: CoursePeriod,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    // 获取时段标签
    val timeOfDayLabel = when (period.timeOfDay) {
        takagi.ru.saison.domain.model.TimeOfDay.MORNING -> "上午"
        takagi.ru.saison.domain.model.TimeOfDay.AFTERNOON -> "下午"
        takagi.ru.saison.domain.model.TimeOfDay.EVENING -> "晚上"
    }
    
    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier.fillMaxWidth(),
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第${period.periodNumber}节",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = timeOfDayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "${period.startTime.format(timeFormatter)} - ${period.endTime.format(timeFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LunchBreakIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = "午休",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "午休",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DinnerBreakIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = "晚休",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "晚休",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
