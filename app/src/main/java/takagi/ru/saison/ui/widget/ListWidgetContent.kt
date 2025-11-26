package takagi.ru.saison.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import takagi.ru.saison.R
import takagi.ru.saison.ui.widget.model.TaskWidgetData
import takagi.ru.saison.ui.widget.model.WidgetTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 配色 - 与应用内一致
private val HighEndBg = Color(0xFF121212)        // 深黑背景
private val CardBg = Color(0xFF2C2C2E)           // 卡片背景色 (surfaceContainerHigh)
private val TextPrimary = Color(0xFFFFFFFF)      // 主标题白
private val TextSecondary = Color(0xFF8E8E93)    // 次要信息灰
private val BrandBlue = Color(0xFF4A90E2)        // 品牌蓝色

// 优先级颜色 - 与应用内 TaskCard 一致
private val PriorityLow = Color(0xFF4CAF50)      // 绿色
private val PriorityMedium = Color(0xFF2196F3)   // 蓝色
private val PriorityHigh = Color(0xFFFF9800)     // 橙色
private val PriorityUrgent = Color(0xFFF44336)   // 红色

/**
 * 纯列表视图小组件内容 - 高级感设计
 * 所有尺寸都显示任务列表，支持交互（完成、星标）
 * 
 * 设计特点：
 * - 极致深黑背景 (#0F0F0F)
 * - Dashboard 风格头部（标题 + 日期 + 胶囊按钮）
 * - 苹果风格卡片 (#1C1C1E)
 * - 更大的内边距和呼吸感
 * - 清晰的信息层级
 */
@Composable
fun ListWidgetContent(data: TaskWidgetData) {
    // 获取当前日期
    val dateStr = try {
        SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA).format(Date())
    } catch (e: Exception) {
        SimpleDateFormat("MM/dd EEEE", Locale.getDefault()).format(Date())
    }
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(HighEndBg))  // 极致深黑
            .cornerRadius(24.dp)  // 更大的圆角
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(20.dp)  // 增加整体内边距，显得不局促
        ) {
            // === 顶部 Dashboard 风格 Header ===
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // 左侧：标题 + 日期
                Column {
                    Text(
                        text = "待办清单",
                        style = TextStyle(
                            color = ColorProvider(TextPrimary),
                            fontSize = 22.sp,  // 字号加大
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = dateStr,  // 显示 "11月25日 星期二"
                        style = TextStyle(
                            color = ColorProvider(TextSecondary),
                            fontSize = 12.sp
                        )
                    )
                }
                
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // 右侧：高亮的添加按钮（胶囊形状）
                Row(
                    modifier = GlanceModifier
                        .background(ColorProvider(BrandBlue))
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionRunCallback<OpenCreateTaskAction>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_add),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "新任务",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.height(20.dp))  // 拉开标题和列表的距离
            
            // === 列表区域 ===
            if (data.allTasks.isEmpty()) {
                // 空状态设计
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
                            colorFilter = ColorFilter.tint(ColorProvider(TextSecondary)),
                            modifier = GlanceModifier.size(48.dp)
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = "今日无任务，享受生活 ✨",
                            style = TextStyle(
                                color = ColorProvider(TextSecondary),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // 按照规则排序：未完成在前，已完成在后；每组内标星在前
                    // 排序优先级：1. 完成状态 2. 收藏状态
                    val sortedTasks = data.allTasks.sortedWith(
                        compareBy<WidgetTask> { it.isCompleted }      // false(未完成)在前，true(已完成)在后
                            .thenByDescending { it.isFavorite }       // true(已标星)在前，false(未标星)在后
                    )
                    
                    // 使用稳定的Long类型itemId，确保Glance能正确追踪每个item
                    // 这对于action的正确触发至关重要
                    items(
                        items = sortedTasks,
                        itemId = { task -> task.id }  // task.id是Long类型，提供稳定标识
                    ) { task ->
                        Column {
                            PremiumTaskItem(task)
                            Spacer(modifier = GlanceModifier.height(10.dp))  // 卡片间距
                        }
                    }
                }
            }
        }
    }
}

/**
 * 任务卡片组件 - 重构版，确保所有交互都能正常工作
 * 
 * 设计要点：
 * - 圆形复选框（蓝色）- 独立点击区域
 * - 任务标题 - 点击打开详情
 * - 右侧星标按钮 - 独立点击区域
 * 
 * 关键修复：
 * 1. 移除外层Box的clickable，避免拦截内部点击
 * 2. 确保复选框和星标的Box有足够大的点击区域(40dp)
 * 3. 任务标题单独添加clickable用于打开详情
 * 4. 使用actionParametersOf明确传递task.id参数
 */
@Composable
fun PremiumTaskItem(task: WidgetTask) {
    // 已完成任务使用更暗的背景色
    val bgColor = if (task.isCompleted) {
        Color(0xFF1C1C1E)  // 更暗的灰色
    } else {
        CardBg
    }
    
    // 外层Box只负责背景和圆角，不添加clickable
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(bgColor))
            .cornerRadius(12.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // 1. 复选框 - 独立的点击区域，40dp确保易于点击
            val checkIcon = if (task.isCompleted) {
                R.drawable.ic_check_circle
            } else {
                R.drawable.ic_circle_outline
            }
            
            Box(
                modifier = GlanceModifier
                    .size(40.dp)  // 40dp点击区域
                    .clickable(
                        actionRunCallback<ToggleCompleteAction>(
                            actionParametersOf(TaskWidgetActionKeys.TASK_ID_KEY to task.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(checkIcon),
                    contentDescription = if (task.isCompleted) "已完成" else "未完成",
                    colorFilter = ColorFilter.tint(ColorProvider(BrandBlue)),
                    modifier = GlanceModifier.size(28.dp)
                )
            }
            
            Spacer(modifier = GlanceModifier.width(12.dp))
            
            // 2. 任务标题 - 点击打开任务详情
            // 使用ActionCallback来处理点击，避免deep link配置问题
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionRunCallback<OpenTaskDetailAction>(
                            actionParametersOf(TaskWidgetActionKeys.TASK_ID_KEY to task.id)
                        )
                    )
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        color = ColorProvider(
                            if (task.isCompleted) TextSecondary else TextPrimary
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                    ),
                    maxLines = 1
                )
            }
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            
            // 3. 右侧星标 - 独立的点击区域，40dp确保易于点击
            Box(
                modifier = GlanceModifier
                    .size(40.dp)  // 40dp点击区域
                    .clickable(
                        actionRunCallback<ToggleStarAction>(
                            actionParametersOf(TaskWidgetActionKeys.TASK_ID_KEY to task.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(
                        if (task.isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline
                    ),
                    contentDescription = if (task.isFavorite) "已标星" else "未标星",
                    colorFilter = ColorFilter.tint(
                        ColorProvider(if (task.isFavorite) BrandBlue else Color(0xFF555555))
                    ),
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }
    }
}
