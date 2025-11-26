package takagi.ru.saison.ui.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import takagi.ru.saison.MainActivity
import takagi.ru.saison.R
import takagi.ru.saison.ui.widget.model.TaskWidgetData
import takagi.ru.saison.ui.widget.model.WidgetTask

/**
 * 大号任务小组件内容 (4x4)
 * 显示统计卡片和完整任务列表
 * 
 * 设计特点：
 * - 深色背景 (#121212)
 * - 顶部蓝色统计卡片
 * - 可滚动任务列表
 * - 右下角蓝色 FAB 按钮
 */
@Composable
fun LargeWidgetContent(data: TaskWidgetData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(TaskWidgetTheme.DarkBackground)
            .cornerRadius(24.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 统计卡片
            StatisticsCard(
                completedToday = data.completedTodayCount,
                incomplete = data.incompleteCount
            )
            
            Spacer(GlanceModifier.height(16.dp))
            
            // 任务列表标题
            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "进行中",
                    style = TextStyle(
                        color = ColorProvider(TaskWidgetTheme.TextPrimary),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    modifier = GlanceModifier
                        .background(TaskWidgetTheme.BrandBlue)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = data.incompleteCount.toString(),
                        style = TextStyle(
                            color = ColorProvider(TaskWidgetTheme.TextPrimary),
                            fontSize = 10.sp
                        )
                    )
                }
            }
            
            Spacer(GlanceModifier.height(8.dp))
            
            // 任务列表
            if (data.incompleteTasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_sparkle),
                            contentDescription = null,
                            modifier = GlanceModifier.size(48.dp)
                        )
                        Spacer(GlanceModifier.height(8.dp))
                        Text(
                            text = "还没有任务",
                            style = TextStyle(
                                color = ColorProvider(TaskWidgetTheme.TextSecondary),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(data.incompleteTasks) { task ->
                        LargeTaskItem(task)
                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        }
        
        // FAB 按钮
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(
                modifier = GlanceModifier
                    .background(TaskWidgetTheme.BrandBlue)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse("saison://task/create")
                            ).apply {
                                setClassName("takagi.ru.saison", "takagi.ru.saison.MainActivity")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    ),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "添加",
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = "添加",
                    style = TextStyle(
                        color = ColorProvider(TaskWidgetTheme.TextPrimary),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * 统计卡片
 */
@Composable
fun StatisticsCard(completedToday: Int, incomplete: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(80.dp)
            .background(TaskWidgetTheme.BrandBlue)
            .cornerRadius(16.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 今日完成
        StatItem(
            iconRes = R.drawable.ic_check_circle,
            count = completedToday.toString(),
            label = "今日完成"
        )
        
        Spacer(GlanceModifier.defaultWeight())
        
        // 分割线
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .height(24.dp)
                .background(ColorProvider(TaskWidgetTheme.TextPrimary.copy(alpha = 0.3f))),
            content = {}
        )
        
        Spacer(GlanceModifier.defaultWeight())
        
        // 待完成
        StatItem(
            iconRes = R.drawable.ic_pending,
            count = incomplete.toString(),
            label = "待完成"
        )
    }
}

/**
 * 统计项
 */
@Composable
fun StatItem(iconRes: Int, count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(24.dp)
        )
        Text(
            text = count,
            style = TextStyle(
                color = ColorProvider(TaskWidgetTheme.TextPrimary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(TaskWidgetTheme.TextPrimary.copy(alpha = 0.8f)),
                fontSize = 10.sp
            )
        )
    }
}

/**
 * 大号组件任务项
 */
@Composable
fun LargeTaskItem(task: WidgetTask) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(TaskWidgetTheme.CardSurface)
            .cornerRadius(12.dp)
            .padding(12.dp)
            .clickable(
                actionStartActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("saison://task/detail/${task.id}")
                    ).apply {
                        setClassName("takagi.ru.saison", "takagi.ru.saison.MainActivity")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // 复选框图标
        Image(
            provider = ImageProvider(R.drawable.ic_circle_outline),
            contentDescription = "未完成",
            modifier = GlanceModifier.size(20.dp)
        )
        
        Spacer(GlanceModifier.width(12.dp))
        
        // 任务标题
        Text(
            text = task.title,
            style = TextStyle(
                color = ColorProvider(TaskWidgetTheme.TextPrimary),
                fontSize = 14.sp
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )
        
        // 星标图标（如果有）
        if (task.isFavorite) {
            Spacer(GlanceModifier.width(8.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_star),
                contentDescription = "星标",
                modifier = GlanceModifier.size(18.dp)
            )
        }
    }
}
