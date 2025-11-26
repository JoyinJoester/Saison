package takagi.ru.saison.ui.screens.task

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.ui.components.TaskCard
import takagi.ru.saison.ui.components.SwipeableTaskCard
import takagi.ru.saison.ui.components.StickyGroupHeader
import takagi.ru.saison.ui.components.CompletedTasksDivider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onTaskClick: (Long) -> Unit,
    onTaskEdit: (Long) -> Unit = {},
    onNavigateToAddTask: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToRoutine: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val incompleteCount by viewModel.incompleteCount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedTasks by viewModel.selectedTasks.collectAsState()
    val groupMode by viewModel.groupMode.collectAsState()
    val isCompletedExpanded by viewModel.isCompletedExpanded.collectAsState()
    val currentItemType by viewModel.currentItemType.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    
    var showNaturalLanguageDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showItemTypeSelector by remember { mutableStateOf(false) }
    
    // 分离已完成和未完成任务
    val (completedTasks, incompleteTasks) = remember(tasks) {
        tasks.partition { it.isCompleted }
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (isMultiSelectMode) {
                MultiSelectTopBar(
                    selectedCount = selectedTasks.size,
                    onClose = { viewModel.exitMultiSelectMode() },
                    onDeleteSelected = { viewModel.deleteSelectedTasks() }
                )
            } else {
                TaskListTopBar(
                    currentItemType = currentItemType,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onFilterClick = { showFilterMenu = true },
                    onItemTypeSelectorClick = { showItemTypeSelector = true }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isMultiSelectMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showNaturalLanguageDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add)) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 统计卡片
            val completionRate = remember(tasks) {
                viewModel.calculateCompletionRate(tasks)
            }
            val todayCompletedCount = remember(tasks) {
                viewModel.calculateTodayCompletedCount(tasks)
            }
            val favoriteCount = remember(tasks) {
                tasks.count { it.isFavorite }
            }
            
            TaskStatsCard(
                incompleteCount = incompleteCount,
                overdueCount = overdueCount,
                completionRate = completionRate,
                todayCompletedCount = todayCompletedCount,
                favoriteCount = favoriteCount,
                modifier = Modifier.padding(16.dp)
            )
            
            // 过滤器标签
            val taskCounts = remember(tasks) {
                mapOf(
                    TaskFilterMode.ALL to tasks.size,
                    TaskFilterMode.ACTIVE to tasks.count { !it.isCompleted },
                    TaskFilterMode.COMPLETED to tasks.count { it.isCompleted },
                    TaskFilterMode.FAVORITE to tasks.count { it.isFavorite }
                )
            }
            
            FilterChips(
                selectedMode = filterMode,
                onModeSelected = { viewModel.setFilterMode(it) },
                taskCounts = taskCounts
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 任务列表
            when {
                isInitialLoading -> {
                    // 初始加载状态 - 显示加载指示器而不是空状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                tasks.isEmpty() && searchQuery.isEmpty() -> {
                    EmptyTaskList(
                        filterMode = filterMode,
                        onCreateTask = { showNaturalLanguageDialog = true }
                    )
                }
                tasks.isEmpty() && searchQuery.isNotEmpty() -> {
                    EmptySearchResult(
                        query = searchQuery,
                        onClearSearch = { viewModel.setSearchQuery("") }
                    )
                }
                else -> {
                    val listState = rememberLazyListState()
                    val isFabVisible by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex == 0
                        }
                    }
                    
                    // 根据分组模式显示任务
                    when (groupMode) {
                        GroupMode.DATE -> {
                            val groupedTasks = remember(tasks) {
                                viewModel.groupTasksByDate(tasks)
                            }
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 88.dp // 为浮动按钮留出空间
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                groupedTasks.forEach { (dateGroup, groupTasks) ->
                                    item(key = "header_${dateGroup.order}") {
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            StickyGroupHeader(
                                                groupName = dateGroup.displayName,
                                                taskCount = groupTasks.size
                                            )
                                        }
                                    }
                                    items(
                                        items = groupTasks,
                                        key = { it.id },
                                        contentType = { "task" }
                                    ) { task ->
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            SwipeableTaskCard(
                                                onSwipeToComplete = {
                                                    viewModel.toggleTaskCompletion(task.id, true)
                                                },
                                                onSwipeToEdit = {
                                                    onTaskEdit(task.id)
                                                },
                                                onSwipeToDelete = {
                                                    viewModel.deleteTask(task.id)
                                                },
                                                modifier = Modifier.animateItemPlacement(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            ) {
                                                TaskCard(
                                                    task = task,
                                                    onTaskClick = { onTaskClick(task.id) },
                                                    onToggleComplete = { isCompleted ->
                                                        viewModel.toggleTaskCompletion(task.id, isCompleted)
                                                    },
                                                    onToggleFavorite = { taskId ->
                                                        viewModel.toggleFavorite(taskId)
                                                    },
                                                    onLongPress = {
                                                        viewModel.enterMultiSelectMode()
                                                        viewModel.toggleTaskSelection(task.id)
                                                    },
                                                    isMultiSelectMode = isMultiSelectMode,
                                                    isSelected = selectedTasks.contains(task.id),
                                                    onSelectionToggle = {
                                                        viewModel.toggleTaskSelection(task.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 88.dp // 为浮动按钮留出空间
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 未完成任务
                                items(
                                    items = incompleteTasks,
                                    key = { it.id },
                                    contentType = { "task" }
                                ) { task ->
                                    SwipeableTaskCard(
                                        onSwipeToComplete = {
                                            viewModel.toggleTaskCompletion(task.id, true)
                                        },
                                        onSwipeToEdit = {
                                            onTaskEdit(task.id)
                                        },
                                        onSwipeToDelete = {
                                            viewModel.deleteTask(task.id)
                                        },
                                        modifier = Modifier.animateItemPlacement(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    ) {
                                        TaskCard(
                                            task = task,
                                            onTaskClick = { onTaskClick(task.id) },
                                            onToggleComplete = { isCompleted ->
                                                viewModel.toggleTaskCompletion(task.id, isCompleted)
                                            },
                                            onToggleFavorite = { taskId ->
                                                viewModel.toggleFavorite(taskId)
                                            },
                                            onLongPress = {
                                                viewModel.enterMultiSelectMode()
                                                viewModel.toggleTaskSelection(task.id)
                                            },
                                            isMultiSelectMode = isMultiSelectMode,
                                            isSelected = selectedTasks.contains(task.id),
                                            onSelectionToggle = {
                                                viewModel.toggleTaskSelection(task.id)
                                            }
                                        )
                                    }
                                }
                                
                                // 已完成任务分隔线
                                if (completedTasks.isNotEmpty()) {
                                    item(key = "completed_divider") {
                                        CompletedTasksDivider(
                                            completedCount = completedTasks.size,
                                            isExpanded = isCompletedExpanded,
                                            onToggleExpanded = { viewModel.toggleCompletedExpanded() }
                                        )
                                    }
                                }
                                
                                // 已完成任务（可折叠）
                                if (isCompletedExpanded && completedTasks.isNotEmpty()) {
                                    items(
                                        items = completedTasks,
                                        key = { it.id },
                                        contentType = { "task" }
                                    ) { task ->
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            SwipeableTaskCard(
                                                onSwipeToComplete = {
                                                    viewModel.toggleTaskCompletion(task.id, false)
                                                },
                                                onSwipeToEdit = {
                                                    onTaskEdit(task.id)
                                                },
                                                onSwipeToDelete = {
                                                    viewModel.deleteTask(task.id)
                                                },
                                                modifier = Modifier.animateItemPlacement(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            ) {
                                                TaskCard(
                                                    task = task,
                                                    onTaskClick = { onTaskClick(task.id) },
                                                    onToggleComplete = { isCompleted ->
                                                        viewModel.toggleTaskCompletion(task.id, isCompleted)
                                                    },
                                                    onToggleFavorite = { taskId ->
                                                        viewModel.toggleFavorite(taskId)
                                                    },
                                                    onLongPress = {
                                                        viewModel.enterMultiSelectMode()
                                                        viewModel.toggleTaskSelection(task.id)
                                                    },
                                                    isMultiSelectMode = isMultiSelectMode,
                                                    isSelected = selectedTasks.contains(task.id),
                                                    onSelectionToggle = {
                                                        viewModel.toggleTaskSelection(task.id)
                                                    }
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
        }
        
        // 添加任务Bottom Sheet
        if (showNaturalLanguageDialog) {
            takagi.ru.saison.ui.components.AddTaskSheet(
                onDismiss = { showNaturalLanguageDialog = false },
                onTaskAdd = { title, dueDateTime, priority, tags, repeatType, reminderEnabled, weekDays ->
                    viewModel.createTask(title, dueDateTime, priority, tags, repeatType, reminderEnabled, weekDays)
                    showNaturalLanguageDialog = false
                },
                parser = takagi.ru.saison.util.NaturalLanguageParser()
            )
        }
        
        // 项目类型选择器 Bottom Sheet
        if (showItemTypeSelector) {
            takagi.ru.saison.ui.components.ItemTypeSelectorBottomSheet(
                currentType = currentItemType,
                onDismiss = { showItemTypeSelector = false },
                onTypeSelected = { type ->
                    showItemTypeSelector = false
                    when (type) {
                        takagi.ru.saison.domain.model.ItemType.EVENT -> {
                            onNavigateToEvents()
                        }
                        takagi.ru.saison.domain.model.ItemType.SCHEDULE -> {
                            onNavigateToRoutine()
                        }
                        takagi.ru.saison.domain.model.ItemType.TASK -> {
                            // 保持在当前页面
                        }
                    }
                }
            )
        }
        
        // 错误提示
        when (val state = uiState) {
            is TaskUiState.Error -> {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(state.message)
                }
            }
            else -> {}
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.task_multi_select_title, selectedCount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
            }
        },
        actions = {
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListTopBar(
    currentItemType: takagi.ru.saison.domain.model.ItemType,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onItemTypeSelectorClick: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.task_search_placeholder)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                Surface(
                    onClick = onItemTypeSelectorClick,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(currentItemType.getDisplayNameResId()),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.cd_dropdown_icon),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = stringResource(if (isSearchActive) R.string.cd_close_search else R.string.cd_search)
                )
            }
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.cd_filter))
            }
        }
    )
}

@Composable
private fun TaskStatsCard(
    incompleteCount: Int,
    overdueCount: Int,
    modifier: Modifier = Modifier,
    completionRate: Float = 0f,
    todayCompletedCount: Int = 0,
    favoriteCount: Int = 0
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // 今日完成进度
            StatItemWithProgress(
                icon = Icons.Default.CheckCircle,
                label = stringResource(R.string.task_stats_today_completed),
                count = todayCompletedCount,
                progress = completionRate
            )
            
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            )
            
            // 未完成任务
            StatItem(
                icon = Icons.Default.Task,
                label = stringResource(R.string.task_stats_incomplete),
                count = incompleteCount
            )
            
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            )
            
            // 已标星
            StatItem(
                icon = Icons.Default.Star,
                label = stringResource(R.string.task_stats_favorite),
                count = favoriteCount
            )
            
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            )
            
            // 逾期任务
            StatItem(
                icon = Icons.Default.Warning,
                label = stringResource(R.string.task_stats_overdue),
                count = overdueCount,
                isWarning = overdueCount > 0
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
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
            count = count,
            isWarning = isWarning
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun StatItemWithProgress(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedCounter(count = count, isWarning = false)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun AnimatedCounter(
    count: Int,
    isWarning: Boolean
) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                slideInVertically { -it } + fadeIn() togetherWith
                slideOutVertically { it } + fadeOut()
            } else {
                slideInVertically { it } + fadeIn() togetherWith
                slideOutVertically { -it } + fadeOut()
            }
        },
        label = "counter"
    ) { animatedCount ->
        Text(
            text = animatedCount.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FilterChips(
    selectedMode: TaskFilterMode,
    onModeSelected: (TaskFilterMode) -> Unit,
    modifier: Modifier = Modifier,
    taskCounts: Map<TaskFilterMode, Int> = emptyMap()
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 进行中
        SegmentedButton(
            selected = selectedMode == TaskFilterMode.ACTIVE,
            onClick = { 
                // 点击已选中的按钮则取消选择，显示全部
                if (selectedMode == TaskFilterMode.ACTIVE) {
                    onModeSelected(TaskFilterMode.ALL)
                } else {
                    onModeSelected(TaskFilterMode.ACTIVE)
                }
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = { 
                SegmentedButtonDefaults.Icon(active = selectedMode == TaskFilterMode.ACTIVE)
            }
        ) {
            Text(text = stringResource(R.string.task_filter_active))
        }
        
        // 已完成
        SegmentedButton(
            selected = selectedMode == TaskFilterMode.COMPLETED,
            onClick = { 
                if (selectedMode == TaskFilterMode.COMPLETED) {
                    onModeSelected(TaskFilterMode.ALL)
                } else {
                    onModeSelected(TaskFilterMode.COMPLETED)
                }
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = { 
                SegmentedButtonDefaults.Icon(active = selectedMode == TaskFilterMode.COMPLETED)
            }
        ) {
            Text(text = stringResource(R.string.task_filter_completed))
        }
        
        // 已标星
        SegmentedButton(
            selected = selectedMode == TaskFilterMode.FAVORITE,
            onClick = { 
                if (selectedMode == TaskFilterMode.FAVORITE) {
                    onModeSelected(TaskFilterMode.ALL)
                } else {
                    onModeSelected(TaskFilterMode.FAVORITE)
                }
            },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = { 
                SegmentedButtonDefaults.Icon(active = selectedMode == TaskFilterMode.FAVORITE)
            }
        ) {
            Text(text = stringResource(R.string.task_filter_favorite))
        }
    }
}

@Composable
private fun EmptyTaskList(
    filterMode: TaskFilterMode,
    onCreateTask: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Material 3 icon - 增大到120dp
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 主标题
            Text(
                text = stringResource(when (filterMode) {
                    TaskFilterMode.ALL -> R.string.task_empty_no_tasks
                    TaskFilterMode.ACTIVE -> R.string.task_empty_no_active
                    TaskFilterMode.COMPLETED -> R.string.task_empty_no_completed
                    TaskFilterMode.FAVORITE -> R.string.task_empty_no_favorite
                }),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 副标题
            Text(
                text = stringResource(R.string.task_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    onClearSearch: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Material 3 icon - 增大到120dp
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 主标题
            Text(
                text = stringResource(R.string.task_search_no_results),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 搜索关键词
            Text(
                text = stringResource(R.string.task_search_query_format, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 清除搜索按钮
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onClearSearch
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除搜索")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NaturalLanguageInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.task_dialog_quick_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.task_dialog_example_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.task_dialog_example_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.task_dialog_description_label)) },
                minLines = 2,
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = { /* TODO: Voice input */ }) {
                        Icon(Icons.Default.Mic, contentDescription = "语音输入")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(input) },
                    enabled = input.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
}
