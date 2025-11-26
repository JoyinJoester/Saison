package takagi.ru.saison.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * 空状态 UI 组件
 * 使用较浅的灰色文本以区别于正常课程内容，同时确保在深色背景上可读
 */
@Composable
fun EmptyState(message: String = "今日无课") {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(Color.White.copy(alpha = 0.5f)), // 较浅的灰色文本
                textAlign = TextAlign.Center
            )
        )
    }
}
