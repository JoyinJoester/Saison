package takagi.ru.saison.ui.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import takagi.ru.saison.MainActivity
import takagi.ru.saison.R
import takagi.ru.saison.ui.widget.model.WidgetCourse
import takagi.ru.saison.ui.widget.model.WidgetData

/**
 * 小组件主内容
 */
@Composable
fun CourseWidgetContent(widgetData: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF141414)))
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent().setClassName(
                "takagi.ru.saison",
                "takagi.ru.saison.MainActivity"
            )))
    ) {
        if (!widgetData.hasActiveSemester) {
            // 无学期状态
            NoSemesterView()
        } else {
            // 顶部信息栏
            TopInfoBar(
                week = widgetData.week,
                date = widgetData.date,
                dayOfWeek = widgetData.dayOfWeek
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // 主内容区域：左右分栏
            Row(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // 左侧：今天 (高亮卡片)
                DayColumn(
                    title = "今天",
                    courses = widgetData.todayCourses,
                    isToday = true,
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                // 右侧：明天 (次要卡片)
                DayColumn(
                    title = "明天",
                    courses = widgetData.tomorrowCourses,
                    isToday = false,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

/**
 * 顶部信息栏 - Pill样式周数 + 右对齐日期
 */
@Composable
fun TopInfoBar(
    week: String,
    date: String,
    dayOfWeek: String
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：周数 Pill
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(Color(0xFF333333)))
                .cornerRadius(12.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = week,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.White)
                )
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        // 右侧：日期
        Text(
            text = "$date $dayOfWeek",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = ColorProvider(Color(0xFFCCCCCC))
            )
        )
    }
}

/**
 * 单天课程列表列 - 卡片式设计
 */
@Composable
fun DayColumn(
    title: String,
    courses: List<WidgetCourse>,
    isToday: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val bgColor = if (isToday) Color(0xFF1E1E1E) else Color(0xFF1E1E1E)
    val titleColor = if (isToday) Color(0xFFD0BCFF) else Color(0xFF999999)
    
    Column(
        modifier = modifier
            .fillMaxHeight() // 【关键】高度撑满，解决对齐问题
            .background(ColorProvider(bgColor))
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        // 标题
        Text(
            text = title,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(titleColor)
            ),
            modifier = GlanceModifier.padding(bottom = 12.dp)
        )

        if (courses.isEmpty()) {
            // 空状态 - 今天的列显示"课程已结束",明天的显示"没有课"
            Text(
                text = if (isToday) "今日课程已结束" else "",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color(0xFFC4C6D0))
                )
            )
        } else {
            // 课程列表 - 使用 forEach 遍历
            courses.forEachIndexed { index, course ->
                CourseItem(course, isToday)
                // 只要不是最后一个，就加间距，解决【卡片粘连】问题
                if (index < courses.lastIndex) {
                    Spacer(GlanceModifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 单个课程卡片 - 带背景的小卡片样式
 */
@Composable
fun CourseItem(course: WidgetCourse, isInTodayColumn: Boolean) {
    // 判断是否是当前正在上的课程
    val isCurrentCourse = course.isCurrent && isInTodayColumn
    
    val bgColor = if (isCurrentCourse) Color(0xFF4F378B) else Color(0xFF333333)
    val textPrimary = ColorProvider(Color(0xFFE6E1E5))
    val textSecondary = ColorProvider(Color(0xFFC4C6D0))
    
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(bgColor))
            .cornerRadius(12.dp)
            .padding(12.dp)
    ) {
        // 课程名称
        Text(
            text = course.name,
            maxLines = 1,
            style = TextStyle(
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 4.dp)
        )
        
        // 地点和时间信息
        val info = buildString {
            if (!course.location.isNullOrEmpty()) {
                append(course.location)
                append(" | ")
            }
            append("${course.startTime} - ${course.endTime}")
        }
        
        Text(
            text = info,
            maxLines = 1,
            style = TextStyle(
                color = textSecondary,
                fontSize = 12.sp
            )
        )
    }
}

/**
 * 空状态视图 - Material 3 风格
 */
@Composable
fun EmptyState(isToday: Boolean = false) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "( ″ ▽ ″ )",
            style = TextStyle(
                fontSize = 24.sp,
                color = ColorProvider(Color(0xFFB8B8B8))
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = if (isToday) "今日课程已结束" else "今天没有课啦",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = ColorProvider(Color(0xFFB8B8B8))
            )
        )
    }
}

/**
 * 无学期状态视图 - Material 3 风格
 */
@Composable
fun NoSemesterView() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📚",
            style = TextStyle(
                fontSize = 32.sp,
                color = ColorProvider(Color(0xFFB8B8B8))
            )
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Text(
            text = "请先创建学期",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color(0xFFB8B8B8))
            )
        )
    }
}
