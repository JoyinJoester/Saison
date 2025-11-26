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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import takagi.ru.saison.MainActivity
import takagi.ru.saison.R
import takagi.ru.saison.ui.widget.model.TaskWidgetData
import takagi.ru.saison.ui.widget.model.WidgetTask

/**
 * 中号任务小组件内容 (4x2)
 * 显示 3-4 个任务清单
 * 
 * 设计特点：
 * - 深色背景 (#121212)
 * - 顶部标题和添加按钮
 * - 最多显示 4 个任务项
 * - 任务项带复选框和星标
 */
@Composable
fun MediumWidgetContent(data: TaskWidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(TaskWidgetTheme.DarkBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
    ) {
        // 顶部标题和添加按钮
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "待办任务",
                style = TextStyle(
                    color = ColorProvider(TaskWidgetTheme.TextPrimary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            // 添加按钮
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(TaskWidgetTheme.BrandBlue)
                    .cornerRadius(16.dp)
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
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "添加任务",
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }
        
        Spacer(GlanceModifier.height(12.dp))
        
        // 任务列表
        if (data.incompleteTasks.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有任务",
                    style = TextStyle(
                        color = ColorProvider(TaskWidgetTheme.TextSecondary),
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(data.incompleteTasks.take(4)) { task ->
                    MediumTaskItem(task)
                    Spacer(GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 中号组件任务项
 */
@Composable
fun MediumTaskItem(task: WidgetTask) {
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
                modifier = GlanceModifier.size(16.dp)
            )
        }
    }
}
