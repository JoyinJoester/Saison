package takagi.ru.saison.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import takagi.ru.saison.domain.model.BreakPeriod
import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.CoursePeriod
import takagi.ru.saison.domain.model.GridLayoutConfig
import takagi.ru.saison.util.buildGridRows
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 网格课程表视图（重构版）
 * 以网格形式显示课程表,支持垂直滚动,全周可见,集成休息时段分隔
 * 
 * @param coursesByDay 按星期分组的课程列表
 * @param periods 节次列表
 * @param breakPeriods 休息时段列表
 * @param semesterStartDate 学期开始日期
 * @param currentWeek 当前周次（1-based）
 * @param onCourseClick 课程点击回调
 * @param onEmptyCellClick 空白单元格点击回调(星期, 节次)
 * @param currentPeriod 当前节次
 * @param currentDay 当前星期
 * @param config 网格布局配置
 * @param weekDays 要显示的星期列表
 * @param autoScrollToCurrentTime 是否自动滚动到当前时间
 * @param onWeekLabelClick 周次标签点击回调
 * @param modifier 修饰符
 */
@Composable
fun GridTimetableView(
    coursesByDay: Map<DayOfWeek, List<Course>>,
    periods: List<CoursePeriod>,
    breakPeriods: List<BreakPeriod> = emptyList(),
    semesterStartDate: LocalDate,
    currentWeek: Int,
    onCourseClick: (Long) -> Unit,
    onEmptyCellClick: (DayOfWeek, Int) -> Unit,
    currentPeriod: Int? = null,
    currentDay: DayOfWeek? = null,
    config: GridLayoutConfig = GridLayoutConfig(),
    weekDays: List<DayOfWeek> = DayOfWeek.values().toList(),
    autoScrollToCurrentTime: Boolean = true,
    onWeekLabelClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // 自动滚动到当前节次
    LaunchedEffect(currentPeriod, autoScrollToCurrentTime) {
        if (autoScrollToCurrentTime && currentPeriod != null && currentPeriod > 0) {
            val targetIndex = (currentPeriod - 1).coerceAtLeast(0)
            val targetOffset = targetIndex * (config.cellHeight.value + 8f)
            coroutineScope.launch {
                scrollState.animateScrollTo(targetOffset.toInt())
            }
        }
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 固定头部: 左上角周次标签 + 日期头部
            Row(modifier = Modifier.fillMaxWidth()) {
                // 左上角显示周次（可点击）
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(config.headerHeight)
                        .clickable { onWeekLabelClick() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "${currentWeek}周",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    WeekDateHeader(
                        semesterStartDate = semesterStartDate,
                        currentWeek = currentWeek,
                        weekDays = weekDays,
                        currentDay = currentDay,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // 可滚动内容: 时间列 + 课程网格(使用Box+绝对定位)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 左侧: 时间列（固定）
                PeriodTimeColumn(
                    periods = periods,
                    breakPeriods = emptyList(), // 不再使用breakPeriods，改用timeOfDay自动检测
                    currentPeriod = currentPeriod,
                    cellHeight = config.cellHeight,
                    showBreakIndicators = config.showBreakSeparators,
                    modifier = Modifier.width(60.dp)
                )
                
                // 右侧: 使用Box实现网格背景+绝对定位课程卡片
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    val density = LocalDensity.current
                    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
                    val cellHeightPx = with(density) { config.cellHeight.toPx() }
                    val cellSpacingPx = with(density) { 4.dp.toPx() }
                    val dayColumnWidth = (maxWidth - 4.dp * (weekDays.size - 1)) / weekDays.size
                    
                    // 计算总高度(包含休息分隔行)
                    val totalHeight = remember(periods, breakPeriods) {
                        var height = 0.dp
                        periods.forEach { period ->
                            val breakBefore = if (config.showBreakSeparators) {
                                breakPeriods.find { it.afterPeriod == period.periodNumber - 1 }
                            } else null
                            if (breakBefore != null) {
                                height += 32.dp
                            }
                            height += config.cellHeight + 4.dp
                        }
                        height
                    }
                    
                    // 绘制网格背景
                    val cellBackgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight)
                    ) {
                        val dayColumnWidthPx = (size.width - (weekDays.size - 1) * cellSpacingPx) / weekDays.size
                        var currentY = 0f
                        
                        periods.forEach { period ->
                            // 检查是否需要绘制休息分隔
                            val breakBefore = if (config.showBreakSeparators) {
                                breakPeriods.find { it.afterPeriod == period.periodNumber - 1 }
                            } else null
                            
                            if (breakBefore != null) {
                                currentY += with(density) { 32.dp.toPx() }
                            }
                            
                            // 绘制每一行的网格单元格
                            weekDays.forEachIndexed { index, _ ->
                                val x = index * (dayColumnWidthPx + cellSpacingPx)
                                
                                // 绘制单元格背景
                                drawRect(
                                    color = cellBackgroundColor,
                                    topLeft = Offset(x, currentY),
                                    size = androidx.compose.ui.geometry.Size(dayColumnWidthPx, cellHeightPx)
                                )
                                
                                // 绘制单元格边框
                                drawRect(
                                    color = gridLineColor,
                                    topLeft = Offset(x, currentY),
                                    size = androidx.compose.ui.geometry.Size(dayColumnWidthPx, cellHeightPx),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                                )
                            }
                            
                            currentY += cellHeightPx + cellSpacingPx
                        }
                    }
                    
                    // 绘制休息分隔行
                    var offsetY = 0.dp
                    periods.forEach { period ->
                        val breakBefore = if (config.showBreakSeparators) {
                            breakPeriods.find { it.afterPeriod == period.periodNumber - 1 }
                        } else null
                        
                        if (breakBefore != null) {
                            BreakSeparatorRow(
                                breakName = breakBefore.name,
                                weekDays = weekDays,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offsetY)
                            )
                            offsetY += 32.dp
                        }
                        offsetY += config.cellHeight + 4.dp
                    }
                    
                    // 绘制空白单元格点击区域
                    var clickOffsetY = 0.dp
                    periods.forEach { period ->
                        val breakBefore = if (config.showBreakSeparators) {
                            breakPeriods.find { it.afterPeriod == period.periodNumber - 1 }
                        } else null
                        
                        if (breakBefore != null) {
                            clickOffsetY += 32.dp
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = clickOffsetY)
                                .height(config.cellHeight),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekDays.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            onEmptyCellClick(day, period.periodNumber)
                                        }
                                )
                            }
                        }
                        
                        clickOffsetY += config.cellHeight + 4.dp
                    }
                    
                    // 绘制课程卡片(使用绝对定位)
                    weekDays.forEachIndexed { dayIndex, day ->
                        val coursesForDay = coursesByDay[day] ?: emptyList()
                        
                        coursesForDay.forEach { course ->
                            if (course.periodStart != null) {
                                // 计算课程卡片的位置
                                var cardOffsetY = 0.dp
                                var periodIndex = 0
                                
                                periods.forEach { period ->
                                    if (period.periodNumber < course.periodStart) {
                                        val breakBefore = if (config.showBreakSeparators) {
                                            breakPeriods.find { it.afterPeriod == period.periodNumber - 1 }
                                        } else null
                                        if (breakBefore != null) {
                                            cardOffsetY += 32.dp
                                        }
                                        cardOffsetY += config.cellHeight + 4.dp
                                        periodIndex++
                                    }
                                }
                                
                                // 检查课程开始节次前是否有休息分隔
                                val breakBeforeStart = if (config.showBreakSeparators) {
                                    breakPeriods.find { it.afterPeriod == course.periodStart - 1 }
                                } else null
                                if (breakBeforeStart != null) {
                                    cardOffsetY += 32.dp
                                }
                                
                                // 计算跨越的节次数
                                val periodSpan = if (course.periodEnd != null && course.periodStart != null) {
                                    (course.periodEnd - course.periodStart + 1).coerceAtLeast(1)
                                } else {
                                    1
                                }
                                
                                val cardOffsetX = (dayColumnWidth + 4.dp) * dayIndex
                                
                                Box(
                                    modifier = Modifier
                                        .offset(x = cardOffsetX, y = cardOffsetY)
                                        .width(dayColumnWidth)
                                ) {
                                    CourseCardCompact(
                                        course = course,
                                        cellHeight = config.cellHeight,
                                        periodSpan = periodSpan,
                                        onClick = { onCourseClick(course.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
