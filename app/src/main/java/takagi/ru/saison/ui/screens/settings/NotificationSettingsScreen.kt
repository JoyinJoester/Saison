package takagi.ru.saison.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.notification.NotificationPermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    permissionManager: NotificationPermissionManager = androidx.hilt.navigation.compose.hiltViewModel<SettingsViewModel>().run {
        // 获取 NotificationPermissionManager 实例
        val context = LocalContext.current
        remember { NotificationPermissionManager(context) }
    },
    quickInputManager: takagi.ru.saison.notification.QuickInputNotificationManager = androidx.hilt.navigation.compose.hiltViewModel<SettingsViewModel>().run {
        // 获取 QuickInputNotificationManager 实例
        val context = LocalContext.current
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        val channelManager = remember { takagi.ru.saison.notification.NotificationChannelManager(context) }
        val permManager = remember { NotificationPermissionManager(context) }
        remember { takagi.ru.saison.notification.QuickInputNotificationManager(context, notificationManager, channelManager, permManager) }
    },
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val taskRemindersEnabled by viewModel.taskRemindersEnabled.collectAsState()
    val courseRemindersEnabled by viewModel.courseRemindersEnabled.collectAsState()
    val pomodoroRemindersEnabled by viewModel.pomodoroRemindersEnabled.collectAsState()
    val quickInputEnabled by viewModel.quickInputEnabled.collectAsState()
    val isPlusActivated by viewModel.isPlusActivated.collectAsState()
    
    // 检查系统通知权限
    var hasNotificationPermission by remember { 
        mutableStateOf(permissionManager.checkNotificationPermission()) 
    }
    
    // 定期检查权限状态
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        hasNotificationPermission = permissionManager.checkNotificationPermission()
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 权限状态提示
            if (!hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "通知权限未授予",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "需要授予通知权限才能接收提醒",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (context is Activity) {
                                    permissionManager.requestNotificationPermission(context)
                                } else {
                                    permissionManager.openNotificationSettings(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("前往设置")
                        }
                    }
                }
            }
            
            // 总开关
            NotificationSettingsSection(title = stringResource(R.string.settings_notifications_general)) {
                NotificationSettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notifications_enable_title),
                    subtitle = stringResource(R.string.settings_notifications_enable_subtitle),
                    checked = notificationsEnabled,
                    enabled = hasNotificationPermission,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
            }
            
            // 具体通知类型
            NotificationSettingsSection(title = stringResource(R.string.settings_notifications_types)) {
                NotificationSettingsSwitchItem(
                    icon = Icons.Default.Task,
                    title = stringResource(R.string.settings_task_reminders_title),
                    subtitle = stringResource(R.string.settings_task_reminders_subtitle),
                    checked = taskRemindersEnabled,
                    enabled = notificationsEnabled && hasNotificationPermission,
                    onCheckedChange = { viewModel.setTaskRemindersEnabled(it) }
                )
                
                NotificationSettingsSwitchItem(
                    icon = Icons.Default.School,
                    title = stringResource(R.string.settings_course_reminders_title),
                    subtitle = stringResource(R.string.settings_course_reminders_subtitle),
                    checked = courseRemindersEnabled,
                    enabled = notificationsEnabled && hasNotificationPermission,
                    onCheckedChange = { viewModel.setCourseRemindersEnabled(it) }
                )
                
                NotificationSettingsSwitchItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_pomodoro_reminders_title),
                    subtitle = stringResource(R.string.settings_pomodoro_reminders_subtitle),
                    checked = pomodoroRemindersEnabled,
                    enabled = notificationsEnabled && hasNotificationPermission,
                    onCheckedChange = { viewModel.setPomodoroRemindersEnabled(it) }
                )
            }
            
            // 快捷输入 (Plus 功能 - 仅在激活后显示)
            if (isPlusActivated) {
                NotificationSettingsSection(title = stringResource(R.string.settings_quick_input_section)) {
                    NotificationSettingsSwitchItem(
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.settings_quick_input_title),
                        subtitle = stringResource(R.string.settings_quick_input_subtitle),
                        checked = quickInputEnabled,
                        enabled = notificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { enabled ->
                            viewModel.setQuickInputEnabled(enabled)
                            // 根据开关状态显示或关闭通知
                            if (enabled) {
                                quickInputManager.showQuickInputNotification()
                            } else {
                                quickInputManager.dismissQuickInputNotification()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun NotificationSettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = title
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            },
            modifier = Modifier.heightIn(min = 56.dp)
        )
    }
}

@Composable
private fun NotificationSettingsSwitchItemWithPlus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    isPlusFeature: Boolean = false,
    isPlusActivated: Boolean = false,
    onPlusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = if (!isPlusActivated && isPlusFeature) {
            { onPlusClick() }
        } else {
            {}
        }
    ) {
        ListItem(
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(title)
                    if (isPlusFeature) {
                        takagi.ru.saison.ui.components.PlusBadge()
                    }
                }
            },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = title
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled
                )
            },
            modifier = Modifier.heightIn(min = 56.dp)
        )
    }
}
