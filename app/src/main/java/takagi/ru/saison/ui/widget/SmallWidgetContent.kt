package takagi.ru.saison.ui.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import takagi.ru.saison.R
import takagi.ru.saison.ui.widget.model.TaskWidgetData

/**
 * 小号任务小组件内容 (2x2)
 * 显示任务数量概览和快速添加按钮
 * 
 * 设计特点：
 * - 纯蓝色背景 (#4A90E2)
 * - 半透明背景纹理增加质感
 * - 白色圆形添加按钮，内部蓝色加号图标（修复白底白字问题）
 */
@Composable
fun SmallWidgetContent(data: TaskWidgetData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(TaskWidgetTheme.BrandBlue)
            .cornerRadius(24.dp)
            .clickable(
                actionStartActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("saison://task/list")
                    ).apply {
                        setClassName("takagi.ru.saison", "takagi.ru.saison.MainActivity")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
            )
    ) {
        // A. 背景装饰纹理 (增加高级感)
        // 一个巨大的半透明图标放在右下角
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_check_circle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.1f))),
                modifier = GlanceModifier
                    .size(100.dp)
                    .padding(bottom = 0.dp, end = (-20).dp)
            )
        }
        
        // B. 主要内容区域
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 顶部标签
            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_pending),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.8f))),
                    modifier = GlanceModifier.size(14.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "待办任务",
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                        fontSize = 12.sp
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            // 核心数字 (大号字体)
            Text(
                text = data.incompleteCount.toString(),
                style = TextStyle(
                    color = ColorProvider(TaskWidgetTheme.TextPrimary),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // C. 右下角悬浮按钮 (修复白点问题)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // 白色圆底
            Box(
                modifier = GlanceModifier
                    .size(42.dp)
                    .background(ColorProvider(Color.White))
                    .cornerRadius(21.dp)
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
                // 蓝色图标 (重点：Tint 设置为 BrandBlue，修复白底白字问题)
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "添加任务",
                    colorFilter = ColorFilter.tint(ColorProvider(TaskWidgetTheme.BrandBlue)),
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }
    }
}
