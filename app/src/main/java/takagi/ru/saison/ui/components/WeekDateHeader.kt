package takagi.ru.saison.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import takagi.ru.saison.util.WeekDateCalculator
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 周日期头部组件
 * 显示周一到周日的日期头部
 *
 * @param semesterStartDate 学期第一周开始日期
 * @param currentWeek 当前周次
 * @param weekDays 要显示的星期列表
 * @param currentDay 当前星期几（用于高亮）
 * @param modifier 修饰符
 */
@Composable
fun WeekDateHeader(
    semesterStartDate: LocalDate,
    currentWeek: Int,
    weekDays: List<DayOfWeek>,
    currentDay: DayOfWeek?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekDays.forEach { day ->
            val date = WeekDateCalculator.getDateForDayInWeek(
                semesterStartDate = semesterStartDate,
                week = currentWeek,
                day = day
            )
            val isToday = date == LocalDate.now()
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = WeekDateCalculator.getDayShortName(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${date.monthValue}/${date.dayOfMonth}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
