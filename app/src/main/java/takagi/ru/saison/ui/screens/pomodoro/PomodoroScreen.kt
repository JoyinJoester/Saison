package takagi.ru.saison.ui.screens.pomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableTasks by viewModel.loadAvailableRoutineTasks().collectAsState(initial = emptyList())
    
    var showTaskSelector by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEarlyFinishDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pomodoro_title)) },
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 圆形计时器
            CircularTimer(
                remainingSeconds = uiState.remainingSeconds,
                totalSeconds = uiState.totalSeconds,
                isRunning = uiState.timerState is TimerState.Running,
                isPaused = uiState.timerState is TimerState.Paused,
                isCompleted = uiState.timerState is TimerState.Completed
            )
            
            // 选中的任务卡片
            AnimatedVisibility(visible = uiState.selectedRoutineTask != null) {
                uiState.selectedRoutineTask?.let { task ->
                    PomodoroRoutineTaskCard(
                        task = task,
                        onChangeTask = { showTaskSelector = true }
                    )
                }
            }
            
            // 控制按钮
            TimerControls(
                timerState = uiState.timerState,
                hasSelectedTask = uiState.selectedRoutineTask != null,
                onSelectTask = { showTaskSelector = true },
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onResume = { viewModel.resumeTimer() },
                onStop = { viewModel.stopTimer() },
                onEarlyFinish = { showEarlyFinishDialog = true }
            )
            
            Divider()
            
            // 今日统计
            TodayStatsCard(stats = uiState.todayStats)
        }
    }
    
    // 任务选择器
    if (showTaskSelector) {
        RoutineTaskSelectorSheet(
            tasks = availableTasks,
            onTaskSelected = { task ->
                viewModel.selectRoutineTask(task)
            },
            onDismiss = { showTaskSelector = false }
        )
    }
    
    // 设置面板
    if (showSettingsSheet) {
        PomodoroSettingsSheet(
            settings = uiState.settings,
            onSettingsChange = { viewModel.updateSettings(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }
    
    // 提前结束对话框
    if (showEarlyFinishDialog) {
        val usedMinutes = (uiState.totalSeconds - uiState.remainingSeconds) / 60
        EarlyFinishDialog(
            usedMinutes = usedMinutes,
            onMarkComplete = {
                viewModel.earlyFinish(markComplete = true)
                showEarlyFinishDialog = false
            },
            onJustStop = {
                viewModel.earlyFinish(markComplete = false)
                showEarlyFinishDialog = false
            },
            onDismiss = { showEarlyFinishDialog = false }
        )
    }
}

@Composable
private fun TimerControls(
    timerState: TimerState,
    hasSelectedTask: Boolean,
    onSelectTask: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onEarlyFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (timerState) {
            is TimerState.Idle, is TimerState.Completed -> {
                // 选择任务按钮（如果未选择）
                if (!hasSelectedTask) {
                    OutlinedButton(
                        onClick = onSelectTask,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pomodoro_select_task))
                    }
                }
                
                // 开始按钮
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pomodoro_action_start))
                }
            }
            
            is TimerState.Running -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 暂停按钮
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pomodoro_action_pause))
                    }
                    
                    // 停止按钮
                    OutlinedButton(
                        onClick = onStop,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                    }
                }
                
                // 提前结束按钮
                TextButton(
                    onClick = onEarlyFinish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pomodoro_early_finish))
                }
            }
            
            is TimerState.Paused -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 继续按钮
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pomodoro_action_resume))
                    }
                    
                    // 停止按钮
                    OutlinedButton(
                        onClick = onStop,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayStatsCard(stats: PomodoroStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.pomodoro_stats_today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    label = stringResource(R.string.pomodoro_stats_completed),
                    value = "${stats.completedSessions}"
                )
                
                StatItem(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.pomodoro_stats_duration),
                    value = stringResource(R.string.pomodoro_stats_minutes_format, stats.totalMinutes)
                )
                
                StatItem(
                    icon = Icons.Default.Warning,
                    label = stringResource(R.string.pomodoro_stats_interruptions),
                    value = "${stats.interruptions}"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
