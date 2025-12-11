package takagi.ru.saison.ui.screens.subscription

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import takagi.ru.saison.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class Subscription(
    val id: Long,
    val name: String,
    val category: String,
    val type: String, // e.g., "按季订阅"
    val accumulatedDuration: String, // e.g., "1月27天"
    val accumulatedCost: Double,
    val nextRenewalDate: LocalDate,
    val status: SubscriptionStatus = SubscriptionStatus.Active,
    val monthlyCost: Double,
    val dailyCost: Double
)

sealed class SubscriptionStatus {
    object Active : SubscriptionStatus()
    data class Overdue(val days: Long) : SubscriptionStatus()
}

@Composable
fun getCycleTypeText(cycleType: String): String {
    return stringResource(
        when(cycleType) {
            "MONTHLY" -> R.string.subscription_cycle_monthly
            "QUARTERLY" -> R.string.subscription_cycle_quarterly
            "YEARLY" -> R.string.subscription_cycle_yearly
            "ONE_TIME" -> R.string.subscription_cycle_onetime
            else -> R.string.subscription_cycle_custom
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: SubscriptionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val filteredSubscriptions by viewModel.filteredSubscriptions.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastSelectedAddCategory by viewModel.lastSelectedAddCategory.collectAsState()
    
    var showAddSheet by remember { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<takagi.ru.saison.data.local.database.entities.SubscriptionEntity?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showCategoryDrawer by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SubscriptionTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSearchToggle = { 
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) {
                        viewModel.setSearchQuery("")
                    }
                },
                onFilterClick = { showCategoryDrawer = true }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.subscription_add_action))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 88.dp // 为浮动按钮留出空间
            )
        ) {
            // 统计卡片 - 常驻显示
            item {
                SubscriptionStatsCard(statistics = statistics)
            }
            
            // 筛选组件 - 常驻显示
            item {
                SubscriptionFilterChips(
                    selectedMode = filterMode,
                    onModeSelected = { viewModel.setFilterMode(it) }
                )
            }
            
            // 空状态提示
            if (filteredSubscriptions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.subscription_empty_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredSubscriptions) { subscription ->
                    val stats = viewModel.calculateStats(subscription)
                    val cycleTypeText = getCycleTypeText(subscription.cycleType)
                    // Map Entity to UI Model on the fly or use Entity directly with helper
                    SubscriptionCard(
                        subscription = Subscription(
                            id = subscription.id,
                            name = subscription.name,
                            category = subscription.category,
                            type = cycleTypeText,
                            accumulatedDuration = stats.accumulatedDuration,
                            accumulatedCost = stats.accumulatedCost,
                            nextRenewalDate = java.time.Instant.ofEpochMilli(subscription.nextRenewalDate)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                            status = stats.status,
                            monthlyCost = stats.averageMonthlyCost,
                            dailyCost = stats.averageDailyCost
                        ),
                        onDelete = { viewModel.deleteSubscription(subscription.id) },
                        onEdit = { 
                            subscriptionToEdit = subscription
                            showAddSheet = true
                        },
                        onClick = {
                            onNavigateToDetail(subscription.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddSubscriptionSheet(
            existingSubscription = subscriptionToEdit,
            categories = categories,
            lastSelectedCategory = lastSelectedAddCategory,
            onDismiss = { 
                showAddSheet = false
                subscriptionToEdit = null
            },
            onSave = { id, name, category, price, cycleType, duration, startDate, endDate, note, autoRenewal, reminderEnabled, reminderDaysBefore ->
                viewModel.saveSubscription(id, name, category, price, cycleType, duration, startDate, endDate, note, autoRenewal, reminderEnabled, reminderDaysBefore)
            },
            onAddCategory = { categoryName ->
                viewModel.addCategory(categoryName)
            }
        )
    }
    
    // 分类筛选抽屉
    if (showCategoryDrawer) {
        Box(modifier = Modifier.fillMaxSize()) {
            CategoryDrawer(
                visible = showCategoryDrawer,
                categories = categories,
                selectedCategory = selectedCategory,
                onDismiss = { showCategoryDrawer = false },
                onCategorySelected = { category ->
                    viewModel.setSelectedCategory(category)
                    showCategoryDrawer = false
                },
                onAddCategory = { categoryName ->
                    viewModel.addCategory(categoryName)
                },
                onDeleteCategory = { category ->
                    viewModel.deleteCategory(category)
                },
                onRenameCategory = { oldName, newName ->
                    viewModel.renameCategory(oldName, newName)
                }
            )
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val maxSwipeDistance = with(density) { 136.dp.toPx() }
    val cardShape = MaterialTheme.shapes.medium
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipe"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
    ) {
        // 背景 - 向右滑动（编辑按钮）
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = if (animatedOffsetX > 0) 1f else 0f
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onEdit()
                        offsetX = 0f
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = cardShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // 背景 - 向左滑动（删除按钮）
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = if (animatedOffsetX < 0) 1f else 0f
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        showDeleteDialog = true
                        offsetX = 0f
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = cardShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.subscription_action_delete),
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // 前景 - 订阅卡片
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-maxSwipeDistance, maxSwipeDistance)
                    },
                    onDragStopped = {
                        val threshold = maxSwipeDistance * 0.3f
                        val buttonWidth = with(density) { 72.dp.toPx() }
                        
                        when {
                            offsetX > threshold -> {
                                offsetX = buttonWidth
                            }
                            offsetX < -threshold -> {
                                offsetX = -buttonWidth
                            }
                            else -> {
                                offsetX = 0f
                            }
                        }
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Name and Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subscription.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "¥${String.format("%.2f", subscription.accumulatedCost)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Row (Type & Duration)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subscription.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.subscription_accumulated_prefix)} ${subscription.accumulatedDuration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer: Renewal & Avg Cost & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Next Renewal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.subscription_next_renewal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = subscription.nextRenewalDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Right: Status, Avg Cost, Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    if (subscription.status is SubscriptionStatus.Overdue) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.subscription_overdue_days, subscription.status.days),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Average Cost
                    // Logic: If type contains "日" or "day" -> Show Daily Avg. Otherwise show Monthly Avg.
                    val perDayText = stringResource(R.string.subscription_per_day)
                    val perMonthText = stringResource(R.string.subscription_per_month)
                    val avgText = if (subscription.type.contains("日") || subscription.type.lowercase().contains("day")) {
                        "¥${String.format("%.2f", subscription.dailyCost)}$perDayText"
                    } else {
                        "¥${String.format("%.2f", subscription.monthlyCost)}$perMonthText"
                    }
                    
                    Text(
                        text = avgText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.subscription_action_delete)) },
            text = { Text(stringResource(R.string.subscription_delete_confirm_message, subscription.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.subscription_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.subscription_cancel_button))
                }
            }
        )
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 统计卡片
@Composable
fun SubscriptionStatsCard(statistics: SubscriptionGlobalStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日均消费
            StatItem(
                icon = Icons.Default.AttachMoney,
                label = stringResource(R.string.subscription_stats_daily_cost),
                value = String.format("¥%.2f", statistics.totalDailyCost)
            )
            
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            )
            
            // 总消费
            StatItem(
                icon = Icons.Default.Category,
                label = "总消费",
                value = String.format("¥%.2f", statistics.totalCost)
            )
            
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            )
            
            // 活跃订阅
            StatItem(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.subscription_stats_active),
                value = statistics.activeCount.toString()
            )
            
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            )
            
            // 逾期订阅
            StatItem(
                icon = Icons.Default.Warning,
                label = stringResource(R.string.subscription_stats_overdue),
                value = statistics.overdueCount.toString(),
                isWarning = statistics.overdueCount > 0
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        AnimatedCounter(
            count = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AnimatedCounter(
    count: String,
    style: androidx.compose.ui.text.TextStyle
) {
    Text(
        text = count,
        style = style
    )
}

// 顶栏组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onFilterClick: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.subscription_search_placeholder)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.subscription_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "关闭搜索" else "搜索"
                )
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// 筛选组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFilterChips(
    selectedMode: SubscriptionFilterMode,
    onModeSelected: (SubscriptionFilterMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SegmentedButton(
            selected = selectedMode == SubscriptionFilterMode.ALL,
            onClick = { onModeSelected(SubscriptionFilterMode.ALL) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
        ) {
            Text(stringResource(R.string.subscription_filter_all))
        }
        
        SegmentedButton(
            selected = selectedMode == SubscriptionFilterMode.ACTIVE,
            onClick = { onModeSelected(SubscriptionFilterMode.ACTIVE) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
        ) {
            Text(stringResource(R.string.subscription_filter_active))
        }
        
        SegmentedButton(
            selected = selectedMode == SubscriptionFilterMode.OVERDUE,
            onClick = { onModeSelected(SubscriptionFilterMode.OVERDUE) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
        ) {
            Text(stringResource(R.string.subscription_filter_overdue))
        }
        
        SegmentedButton(
            selected = selectedMode == SubscriptionFilterMode.PAUSED,
            onClick = { onModeSelected(SubscriptionFilterMode.PAUSED) },
            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
        ) {
            Text(stringResource(R.string.subscription_filter_paused))
        }
    }
}

