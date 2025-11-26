package takagi.ru.saison.ui.screens.routine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.ui.components.RoutineCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 日程打卡主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RoutineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showItemTypeSelector by remember { mutableStateOf(false) }
    
    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        onClick = { showItemTypeSelector = true },
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "日程打卡",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.cd_dropdown_icon),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加日程"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // 加载状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.activeTasks.isEmpty() && uiState.inactiveTasks.isEmpty() -> {
                    // 空状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "还没有日程任务",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { showCreateSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("创建日程")
                            }
                        }
                    }
                }
                
                else -> {
                    // 任务列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 88.dp // 为浮动按钮留出空间
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 活跃任务
                        if (uiState.activeTasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "活跃任务",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            items(
                                items = uiState.activeTasks,
                                key = { it.task.id }
                            ) { taskWithStats ->
                                RoutineCard(
                                    taskWithStats = taskWithStats,
                                    onCheckIn = {
                                        viewModel.checkInTask(taskWithStats.task.id)
                                    },
                                    onClick = {
                                        onNavigateToDetail(taskWithStats.task.id)
                                    }
                                )
                            }
                        }
                        
                        // 非活跃任务
                        if (uiState.inactiveTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "非活跃任务",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            items(
                                items = uiState.inactiveTasks,
                                key = { it.task.id }
                            ) { taskWithStats ->
                                RoutineCard(
                                    taskWithStats = taskWithStats,
                                    onCheckIn = {
                                        // 非活跃任务不应该能打卡，但为了安全起见还是处理
                                        viewModel.checkInTask(taskWithStats.task.id)
                                    },
                                    onClick = {
                                        onNavigateToDetail(taskWithStats.task.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 创建/编辑任务底部表单
    if (showCreateSheet) {
        takagi.ru.saison.ui.components.CreateRoutineSheet(
            onDismiss = { showCreateSheet = false },
            onSave = { task ->
                viewModel.createRoutineTask(task)
                showCreateSheet = false
            }
        )
    }
    
    // 项目类型选择器 Bottom Sheet
    if (showItemTypeSelector) {
        takagi.ru.saison.ui.components.ItemTypeSelectorBottomSheet(
            currentType = takagi.ru.saison.domain.model.ItemType.SCHEDULE,
            onDismiss = { showItemTypeSelector = false },
            onTypeSelected = { type ->
                showItemTypeSelector = false
                when (type) {
                    takagi.ru.saison.domain.model.ItemType.TASK -> onNavigateToTasks()
                    takagi.ru.saison.domain.model.ItemType.EVENT -> onNavigateToEvents()
                    takagi.ru.saison.domain.model.ItemType.SCHEDULE -> {} // 保持在当前页面
                }
            }
        )
    }
}


