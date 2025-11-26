package takagi.ru.saison.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.data.repository.CourseWidgetRepository
import takagi.ru.saison.ui.widget.model.WidgetData
import javax.inject.Inject

class SingleDayWidget @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: CourseWidgetRepository
) : GlanceAppWidget() {

    companion object {
        private const val TAG = "SingleDayWidget"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        android.util.Log.d(TAG, "provideGlance called for widget $id")
        val startTime = System.currentTimeMillis()
        
        // 获取真实的课程数据
        val widgetData = try {
            val data = repository.getWidgetData()
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d(TAG, "Widget data fetched in ${duration}ms: hasActiveSemester=${data.hasActiveSemester}, courses=${data.todayCourses.size}")
            data
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.e(TAG, "Failed to get widget data after ${duration}ms", e)
            WidgetData.empty()
        }

        provideContent {
            SingleDayWidgetContent(widgetData)
        }
    }

    @Composable
    fun SingleDayWidgetContent(widgetData: WidgetData) {
        // 根背景：保持深邃的黑色
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF141414)) // 纯黑背景
                .cornerRadius(24.dp)
                .padding(16.dp)
        ) {
            // ================= Header (左对齐日期) =================
            Column(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // 使用真实的日期数据
                val dateText = if (widgetData.date.isNotEmpty()) {
                    "${widgetData.date.replace(".", "月")}日"
                } else {
                    "无学期"
                }
                
                Text(
                    text = dateText,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE6E1E5)),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                )
                
                // 使用真实的星期和周次数据
                val weekText = if (widgetData.hasActiveSemester) {
                    "${widgetData.dayOfWeek} · ${widgetData.week}"
                } else {
                    "无活动学期"
                }
                
                Text(
                    text = weekText,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF999999)),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start
                    ),
                    modifier = GlanceModifier.padding(top = 2.dp)
                )
            }

            // ================= 课程列表 (透明设计) =================
            if (widgetData.todayCourses.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("今日无课", style = TextStyle(color = ColorProvider(Color(0xFF666666))))
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    itemsIndexed(widgetData.todayCourses) { index, course ->
                        // 渲染课程 Item (透明背景)
                        TransparentCourseItem(
                            name = course.name,
                            location = course.location ?: "",
                            startTime = course.startTime,
                            endTime = course.endTime,
                            isHighlight = course.isCurrent
                        )

                        // 如果不是最后一个，添加间距
                        if (index < widgetData.todayCourses.lastIndex) {
                            Spacer(GlanceModifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TransparentCourseItem(
        name: String,
        location: String,
        startTime: String,
        endTime: String,
        isHighlight: Boolean
    ) {
        // 颜色逻辑：高亮时用紫色文字，普通时用白色/灰色
        val titleColor = if (isHighlight) Color(0xFFD0BCFF) else Color(0xFFE6E1E5)
        val timeColor = if (isHighlight) Color(0xFFD0BCFF) else Color(0xFFE6E1E5) // 时间也高亮
        val roomColor = if (isHighlight) Color(0xFFD0BCFF) else Color(0xFF999999)

        // 只有高亮课程加粗，普通课程标准粗细
        val titleWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                // 注意：这里没有 background 了
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.Bottom
            ) {
                // 课程名
                Text(
                    text = name,
                    style = TextStyle(
                        color = ColorProvider(titleColor),
                        fontSize = 17.sp,
                        fontWeight = titleWeight
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // 时间
                Text(
                    text = "$startTime-$endTime",
                    style = TextStyle(
                        color = ColorProvider(timeColor),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium // 时间字体统一稍微明显一点
                    ),
                    modifier = GlanceModifier.padding(start = 8.dp)
                )
            }

            // 地点
            Text(
                text = location,
                style = TextStyle(
                    color = ColorProvider(roomColor),
                    fontSize = 13.sp
                ),
                modifier = GlanceModifier.padding(top = 4.dp)
            )
        }
    }
}
