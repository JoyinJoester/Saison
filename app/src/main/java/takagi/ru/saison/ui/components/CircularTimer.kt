package takagi.ru.saison.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import takagi.ru.saison.R

@Composable
fun CircularTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    isPaused: Boolean = false,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 20.dp
) {
    val progressColor = when {
        isCompleted -> MaterialTheme.colorScheme.secondary
        isPaused -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    
    // 计算已经过去的时间比例（深色部分）
    val progress = if (totalSeconds > 0) {
        (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()
    } else {
        0f
    }
    
    // 使用 Material 3 推荐的动画规格
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 使用 Material 3 官方的 CircularProgressIndicator
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(size),
            color = progressColor,
            strokeWidth = strokeWidth,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
        )
        
        // 时间显示
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(remainingSeconds),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(
                    when {
                        isCompleted -> R.string.pomodoro_status_completed
                        isRunning -> R.string.pomodoro_status_focusing
                        isPaused -> R.string.pomodoro_status_paused
                        else -> R.string.pomodoro_status_ready
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.secondary
                    isPaused -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
