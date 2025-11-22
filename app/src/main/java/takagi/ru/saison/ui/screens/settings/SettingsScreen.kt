package takagi.ru.saison.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import takagi.ru.saison.R
import takagi.ru.saison.data.local.datastore.SeasonalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToBottomNavSettings: () -> Unit = {},
    onNavigateToImportPreview: (android.net.Uri, Long) -> Unit = { _, _ -> },
    onNavigateToExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    
    var showThemeBottomSheet by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showBottomNavBottomSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    // 获取 Context
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Snackbar 支持
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 文件选择器 - 用于导入JSON文件
    val coroutineScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let {
            // 导航到导入预览界面，传递Uri
            onNavigateToImportPreview(it, 0L) // semesterId参数不再使用
        }
    }
    
    // 文件选择器 - 用于导出JSON文件
    val exportLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        uri?.let {
            coroutineScope.launch {
                val exportOptions = viewModel.exportOptions.value
                if (exportOptions != null) {
                    viewModel.executeExportToUri(it, exportOptions.semesterIds)
                }
            }
        }
    }
    
    // 获取字符串资源（在 Composable 上下文中）
    val confirmText = stringResource(R.string.common_action_confirm)
    
    // 观察 UI 事件
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is SettingsUiEvent.ShowError -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Long,
                            actionLabel = confirmText
                        )
                    }
                }
                is SettingsUiEvent.RestartRequired -> {
                    // 重新创建 Activity 以应用语言更改
                    (context as? android.app.Activity)?.recreate()
                }
                is SettingsUiEvent.NavigateToSystemSettings -> {
                    // TODO: 导航到系统设置
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 外观设置
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = getThemeName(currentTheme),
                    onClick = { showThemeBottomSheet = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme_mode_title),
                    subtitle = getThemeModeName(themeMode),
                    onClick = { showThemeModeDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.ViewWeek,
                    title = stringResource(R.string.settings_bottom_nav_title),
                    subtitle = stringResource(R.string.settings_bottom_nav_subtitle),
                    onClick = { showBottomNavBottomSheet = true }
                )
            }
            
            // 语言设置
            SettingsSection(title = stringResource(R.string.settings_section_language)) {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language_title),
                    subtitle = getLanguageName(currentLanguage),
                    onClick = { showLanguageDialog = true }
                )
            }
            
            // 通知设置
            SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                val taskRemindersEnabled by viewModel.taskRemindersEnabled.collectAsState()
                val courseRemindersEnabled by viewModel.courseRemindersEnabled.collectAsState()
                val pomodoroRemindersEnabled by viewModel.pomodoroRemindersEnabled.collectAsState()
                
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notifications_enable_title),
                    subtitle = stringResource(R.string.settings_notifications_enable_subtitle),
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                
                SettingsSwitchItem(
                    icon = Icons.Default.Task,
                    title = stringResource(R.string.settings_task_reminders_title),
                    subtitle = stringResource(R.string.settings_task_reminders_subtitle),
                    checked = taskRemindersEnabled,
                    enabled = notificationsEnabled,
                    onCheckedChange = { viewModel.setTaskRemindersEnabled(it) }
                )
                
                SettingsSwitchItem(
                    icon = Icons.Default.School,
                    title = stringResource(R.string.settings_course_reminders_title),
                    subtitle = stringResource(R.string.settings_course_reminders_subtitle),
                    checked = courseRemindersEnabled,
                    enabled = notificationsEnabled,
                    onCheckedChange = { viewModel.setCourseRemindersEnabled(it) }
                )
                
                SettingsSwitchItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_pomodoro_reminders_title),
                    subtitle = stringResource(R.string.settings_pomodoro_reminders_subtitle),
                    checked = pomodoroRemindersEnabled,
                    enabled = notificationsEnabled,
                    onCheckedChange = { viewModel.setPomodoroRemindersEnabled(it) }
                )
            }
            
            // 同步设置
            SettingsSection(title = stringResource(R.string.settings_section_sync)) {
                val syncOnWifiOnly by viewModel.syncOnWifiOnly.collectAsState()
                val syncStatus by viewModel.syncStatus.collectAsState()
                
                SettingsSwitchItem(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.settings_auto_sync_title),
                    subtitle = stringResource(R.string.settings_auto_sync_subtitle),
                    checked = autoSyncEnabled,
                    onCheckedChange = { viewModel.setAutoSyncEnabled(it) }
                )
                
                SettingsSwitchItem(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.settings_wifi_only_title),
                    subtitle = stringResource(R.string.settings_wifi_only_subtitle),
                    checked = syncOnWifiOnly,
                    enabled = autoSyncEnabled,
                    onCheckedChange = { viewModel.setSyncOnWifiOnly(it) }
                )
                
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = stringResource(R.string.settings_webdav_title),
                    subtitle = stringResource(R.string.settings_webdav_subtitle),
                    onClick = { showWebDavDialog = true }
                )
                
                // 同步状态和手动同步按钮
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_sync_now_title)) },
                        supportingContent = {
                            Column {
                                if (syncStatus.lastSyncTime != null) {
                                    val timeAgo = remember(syncStatus.lastSyncTime) {
                                        val diff = System.currentTimeMillis() - syncStatus.lastSyncTime!!
                                        when {
                                            diff < 60000 -> context.getString(R.string.time_just_now)
                                            diff < 3600000 -> context.getString(R.string.time_minutes_ago, diff / 60000)
                                            diff < 86400000 -> context.getString(R.string.time_hours_ago, diff / 3600000)
                                            else -> context.getString(R.string.time_days_ago, diff / 86400000)
                                        }
                                    }
                                    Text(stringResource(R.string.settings_sync_last_time, timeAgo))
                                } else {
                                    Text(stringResource(R.string.settings_sync_not_yet))
                                }
                                if (syncStatus.errorMessage != null) {
                                    Text(
                                        text = stringResource(R.string.settings_sync_error, syncStatus.errorMessage!!),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            if (syncStatus.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = stringResource(R.string.cd_sync_now)
                                )
                            }
                        },
                        trailingContent = {
                            Button(
                                onClick = { viewModel.triggerManualSync() },
                                enabled = !syncStatus.isSyncing
                            ) {
                                Text(stringResource(R.string.settings_sync_button))
                            }
                        }
                    )
                }
            }
            
            // 关于
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    onClick = { showAboutDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.settings_licenses_title),
                    subtitle = stringResource(R.string.settings_licenses_subtitle),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
    
    // 主题选择 Bottom Sheet
    if (showThemeBottomSheet) {
        ThemeBottomSheet(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
            },
            onDismiss = { showThemeBottomSheet = false }
        )
    }
    
    // 主题模式选择对话框
    if (showThemeModeDialog) {
        ThemeModeSelectionDialog(
            currentMode = themeMode,
            onModeSelected = { mode ->
                viewModel.setThemeMode(mode)
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }
    
    // 底部导航栏设置 Bottom Sheet
    if (showBottomNavBottomSheet) {
        BottomNavBottomSheet(
            viewModel = viewModel,
            onDismiss = { showBottomNavBottomSheet = false }
        )
    }
    
    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                viewModel.setLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // WebDAV 配置对话框
    if (showWebDavDialog) {
        WebDavConfigDialog(
            viewModel = viewModel,
            onDismiss = { showWebDavDialog = false },
            onSave = { url, username, password ->
                viewModel.setWebDavConfig(url, username, password)
                showWebDavDialog = false
            }
        )
    }
    
    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    
    // 导出选项对话框
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val exportInProgress by viewModel.exportInProgress.collectAsState()
    val allSemesters by viewModel.allSemesters.collectAsState()
    val currentSemesterId by viewModel.currentSemesterId.collectAsState()
    
    if (showExportDialog) {
        takagi.ru.saison.ui.components.ExportDialog(
            semesters = allSemesters,
            currentSemesterId = currentSemesterId,
            onDismiss = { viewModel.dismissExportDialog() },
            onExport = { options ->
                viewModel.dismissExportDialog()
                coroutineScope.launch {
                    // 保存导出选项
                    viewModel.saveExportOptions(options.semesterIds)
                    
                    // 获取建议的文件名
                    val fileName = if (options.semesterIds.size == 1) {
                        viewModel.getSuggestedFileName(options.semesterIds.first())
                    } else {
                        "课程表_${System.currentTimeMillis()}.json"
                    }
                    
                    // 启动文件选择器
                    exportLauncher.launch(fileName)
                }
            }
        )
    }
}

// 3.1 SettingsSection 组件 - 符合 M3 规范，卡片式布局
@Composable
private fun SettingsSection(
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

// 3.2 SettingsItem 组件 - 符合 M3 规范，卡片式布局，支持无障碍
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
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
        onClick = onClick
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
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            },
            modifier = Modifier.heightIn(min = 56.dp)
        )
    }
}

// 3.3 SettingsSwitchItem 组件 - 符合 M3 规范，卡片式布局，支持无障碍和动画
@Composable
private fun SettingsSwitchItem(
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

// 3.4 SettingsSliderItem 组件 - 符合 M3 规范
@Composable
private fun SettingsSliderItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    valueFormatter: (Int) -> String = { "$it" },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

// 4.1 ThemePreviewCard 组件
@Composable
private fun ThemePreviewCard(
    theme: SeasonalTheme,
    themeName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = themeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 主题颜色预览条 - 显示4种代表性颜色
                val themeColors = getThemePreviewColors(theme)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 主色
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                themeColors.primary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    // 次色
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                themeColors.secondary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    // 第三色
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                themeColors.tertiary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    // 表面色
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                themeColors.surface,
                                shape = MaterialTheme.shapes.small
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}

/**
 * ThemeBottomSheet - Material 3 Extended 风格的主题选择 Bottom Sheet
 * 
 * 从屏幕底部滑入的主题选择面板，提供流畅的交互体验和完整的 M3 设计规范支持。
 * 
 * 特性：
 * - 使用 ModalBottomSheet 实现从底部滑入的动画
 * - 支持点击背景遮罩、向下滑动和系统返回键关闭
 * - 响应式设计：自动适配手机和平板设备
 * - 无障碍支持：完整的 TalkBack 和语义化标签
 * - 性能优化：颜色缓存、状态优化、流畅动画
 * - 错误处理：主题应用失败时的异常处理
 * 
 * @param currentTheme 当前选中的主题
 * @param onThemeSelected 主题选择回调，参数为选中的主题
 * @param onDismiss Bottom Sheet 关闭回调
 * @param modifier 可选的 Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeBottomSheet(
    currentTheme: SeasonalTheme,
    onThemeSelected: (SeasonalTheme) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 响应式设计：判断是否为平板
    val isTablet = configuration.screenWidthDp >= 600
    
    // 无障碍支持：宣布 Bottom Sheet 打开
    LaunchedEffect(Unit) {
        // 宣布主题选择已打开
        val accessibilityManager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) 
            as? android.view.accessibility.AccessibilityManager
        if (accessibilityManager?.isEnabled == true) {
            val event = android.view.accessibility.AccessibilityEvent.obtain().apply {
                eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                text.add(context.getString(R.string.settings_theme_dialog_title))
            }
            accessibilityManager.sendAccessibilityEvent(event)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        ),
        modifier = modifier
            .then(
                if (isTablet) {
                    Modifier.widthIn(max = 600.dp)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .semantics {
                contentDescription = context.getString(R.string.settings_theme_dialog_title)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.settings_theme_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // 主题列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = SeasonalTheme.values().toList(),
                    key = { it.name }
                ) { theme ->
                    // 使用 derivedStateOf 优化 isSelected 状态
                    val isSelected by remember {
                        derivedStateOf { theme == currentTheme }
                    }
                    
                    ThemePreviewCard(
                        theme = theme,
                        themeName = getThemeName(theme),
                        isSelected = isSelected,
                        onClick = {
                            // 错误处理：主题应用
                            try {
                                onThemeSelected(theme)
                                
                                // 无障碍支持：宣布主题已选中
                                val accessibilityManager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) 
                                    as? android.view.accessibility.AccessibilityManager
                                if (accessibilityManager?.isEnabled == true) {
                                    val event = android.view.accessibility.AccessibilityEvent.obtain().apply {
                                        eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                                        text.add("已选择${getThemeName(theme)}主题")
                                    }
                                    accessibilityManager.sendAccessibilityEvent(event)
                                }
                                
                                // 关闭 Bottom Sheet
                                scope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        onDismiss()
                                    }
                                }
                            } catch (e: Exception) {
                                // 错误处理将在 SettingsScreen 的 Snackbar 中显示
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }
    }
}



@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "zh" to stringResource(R.string.language_zh_cn),
        "en" to stringResource(R.string.language_en),
        "ja" to stringResource(R.string.language_ja),
        "vi" to stringResource(R.string.language_vi)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_dialog_title)) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == currentLanguage,
                            onClick = { onLanguageSelected(code) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_action_close))
            }
        }
    )
}

// 6.1 WebDavConfigDialog 对话框 - 增强版
@Composable
private fun WebDavConfigDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // URL 格式验证
    val isValidUrl = url.isBlank() || url.startsWith("http://") || url.startsWith("https://")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_webdav_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it
                        testResult = null
                    },
                    label = { Text(stringResource(R.string.settings_webdav_url_label)) },
                    placeholder = { Text(stringResource(R.string.settings_webdav_url_placeholder)) },
                    isError = !isValidUrl,
                    supportingText = if (!isValidUrl) {
                        { Text(stringResource(R.string.settings_webdav_url_error)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        testResult = null
                    },
                    label = { Text(stringResource(R.string.settings_webdav_username_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        testResult = null
                    },
                    label = { Text(stringResource(R.string.settings_webdav_password_label)) },
                    visualTransformation = if (passwordVisible) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = stringResource(if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 测试连接按钮
                Button(
                    onClick = {
                        isTesting = true
                        viewModel.testWebDavConnection(url, username, password)
                    },
                    enabled = url.isNotBlank() && username.isNotBlank() && isValidUrl && !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting && uiState is SettingsUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.settings_webdav_test_button))
                }
                
                // 显示测试结果
                when (uiState) {
                    is SettingsUiState.Success -> {
                        isTesting = false
                        Text(
                            text = stringResource(R.string.settings_webdav_test_success),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is SettingsUiState.Error -> {
                        isTesting = false
                        Text(
                            text = stringResource(R.string.settings_webdav_test_error, (uiState as SettingsUiState.Error).message),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url, username, password) },
                enabled = url.isNotBlank() && username.isNotBlank() && isValidUrl
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 Saison") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Saison 任务管理应用",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("版本: 1.0.0")
                Text("一款优雅的任务管理应用，支持日历、课程表、番茄钟等功能。")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2024 Saison",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun getThemeName(theme: SeasonalTheme): String {
    return when (theme) {
        SeasonalTheme.DYNAMIC -> "动态"
        SeasonalTheme.AUTO_SEASONAL -> "四季 (${takagi.ru.saison.util.SeasonHelper.getCurrentSeasonName()})"
        SeasonalTheme.SAKURA -> "樱花"
        SeasonalTheme.MINT -> "薄荷"
        SeasonalTheme.AMBER -> "琥珀"
        SeasonalTheme.SNOW -> "雪"
        SeasonalTheme.RAIN -> "雨"
        SeasonalTheme.MAPLE -> "枫叶"
        SeasonalTheme.OCEAN -> "海洋"
        SeasonalTheme.SUNSET -> "日落"
        SeasonalTheme.FOREST -> "森林"
        SeasonalTheme.LAVENDER -> "薰衣草"
        SeasonalTheme.DESERT -> "沙漠"
        SeasonalTheme.AURORA -> "极光"
    }
}

private fun getLanguageName(code: String): String {
    return when (code) {
        "zh" -> "简体中文"
        "en" -> "English"
        "ja" -> "日本語"
        "vi" -> "Tiếng Việt"
        else -> "简体中文"
    }
}


// 数据类用于主题颜色预览
private data class ThemePreviewColors(
    val primary: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val tertiary: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color
)

// 获取每个主题的预览颜色
@Composable
private fun getThemePreviewColors(theme: takagi.ru.saison.data.local.datastore.SeasonalTheme): ThemePreviewColors {
    val context = LocalContext.current
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    return when (theme) {
        takagi.ru.saison.data.local.datastore.SeasonalTheme.DYNAMIC -> {
            // 在 Android 12+ 上使用实际的动态颜色
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val dynamicScheme = if (darkTheme) {
                    androidx.compose.material3.dynamicDarkColorScheme(context)
                } else {
                    androidx.compose.material3.dynamicLightColorScheme(context)
                }
                ThemePreviewColors(
                    primary = dynamicScheme.primary,
                    secondary = dynamicScheme.secondary,
                    tertiary = dynamicScheme.tertiary,
                    surface = dynamicScheme.surfaceVariant
                )
            } else {
                // Android 12 以下使用默认颜色
                ThemePreviewColors(
                    primary = takagi.ru.saison.ui.theme.Purple40,
                    secondary = takagi.ru.saison.ui.theme.PurpleGrey40,
                    tertiary = takagi.ru.saison.ui.theme.Pink40,
                    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE)
                )
            }
        }
        takagi.ru.saison.data.local.datastore.SeasonalTheme.AUTO_SEASONAL -> {
            // 显示当前季节的颜色
            val currentSeasonTheme = takagi.ru.saison.util.SeasonHelper.getCurrentSeasonTheme()
            getThemePreviewColors(currentSeasonTheme)
        }
        takagi.ru.saison.data.local.datastore.SeasonalTheme.SAKURA -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.SakuraPrimary,
            secondary = takagi.ru.saison.ui.theme.SakuraSecondary,
            tertiary = takagi.ru.saison.ui.theme.SakuraTertiary,
            surface = takagi.ru.saison.ui.theme.SakuraSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.MINT -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.MintPrimary,
            secondary = takagi.ru.saison.ui.theme.MintSecondary,
            tertiary = takagi.ru.saison.ui.theme.MintTertiary,
            surface = takagi.ru.saison.ui.theme.MintSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.AMBER -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.AmberPrimary,
            secondary = takagi.ru.saison.ui.theme.AmberSecondary,
            tertiary = takagi.ru.saison.ui.theme.AmberTertiary,
            surface = takagi.ru.saison.ui.theme.AmberSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.SNOW -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.SnowPrimary,
            secondary = takagi.ru.saison.ui.theme.SnowSecondary,
            tertiary = takagi.ru.saison.ui.theme.SnowTertiary,
            surface = takagi.ru.saison.ui.theme.SnowSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.RAIN -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.RainPrimary,
            secondary = takagi.ru.saison.ui.theme.RainSecondary,
            tertiary = takagi.ru.saison.ui.theme.RainTertiary,
            surface = takagi.ru.saison.ui.theme.RainSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.MAPLE -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.MaplePrimary,
            secondary = takagi.ru.saison.ui.theme.MapleSecondary,
            tertiary = takagi.ru.saison.ui.theme.MapleTertiary,
            surface = takagi.ru.saison.ui.theme.MapleSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.OCEAN -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.OceanPrimary,
            secondary = takagi.ru.saison.ui.theme.OceanSecondary,
            tertiary = takagi.ru.saison.ui.theme.OceanTertiary,
            surface = takagi.ru.saison.ui.theme.OceanSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.SUNSET -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.SunsetPrimary,
            secondary = takagi.ru.saison.ui.theme.SunsetSecondary,
            tertiary = takagi.ru.saison.ui.theme.SunsetTertiary,
            surface = takagi.ru.saison.ui.theme.SunsetSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.FOREST -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.ForestPrimary,
            secondary = takagi.ru.saison.ui.theme.ForestSecondary,
            tertiary = takagi.ru.saison.ui.theme.ForestTertiary,
            surface = takagi.ru.saison.ui.theme.ForestSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.LAVENDER -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.LavenderPrimary,
            secondary = takagi.ru.saison.ui.theme.LavenderSecondary,
            tertiary = takagi.ru.saison.ui.theme.LavenderTertiary,
            surface = takagi.ru.saison.ui.theme.LavenderSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.DESERT -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.DesertPrimary,
            secondary = takagi.ru.saison.ui.theme.DesertSecondary,
            tertiary = takagi.ru.saison.ui.theme.DesertTertiary,
            surface = takagi.ru.saison.ui.theme.DesertSurfaceVariant
        )
        takagi.ru.saison.data.local.datastore.SeasonalTheme.AURORA -> ThemePreviewColors(
            primary = takagi.ru.saison.ui.theme.AuroraPrimary,
            secondary = takagi.ru.saison.ui.theme.AuroraSecondary,
            tertiary = takagi.ru.saison.ui.theme.AuroraTertiary,
            surface = takagi.ru.saison.ui.theme.AuroraSurfaceVariant
        )
    }
}


@Composable
private fun RestartAppDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重启应用") },
        text = {
            Text("语言设置已更改，需要重启应用才能生效。是否立即重启？")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("立即重启")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}


/**
 * BottomNavBottomSheet - 底部导航栏设置 Bottom Sheet
 * 
 * 从屏幕底部滑入的底部导航栏设置面板，用户可以自定义显示的项目和顺序。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomNavBottomSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavVisibility by viewModel.bottomNavVisibility.collectAsState()
    val bottomNavOrder by viewModel.bottomNavOrder.collectAsState()
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    
    // 响应式设计：判断是否为平板
    val isTablet = configuration.screenWidthDp >= 600
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        ),
        modifier = modifier
            .then(
                if (isTablet) {
                    Modifier.widthIn(max = 600.dp)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .semantics {
                contentDescription = "底部导航栏设置"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 标题
            Text(
                text = "底部导航栏",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // 提示文字
            Text(
                text = "自定义底部导航栏显示的项目和顺序。至少保留一个可见项。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // 导航项列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val visibleCount = bottomNavVisibility.visibleCount()
                
                itemsIndexed(
                    items = bottomNavOrder,
                    key = { _, tab -> tab.name }
                ) { index, tab ->
                    val isVisible = bottomNavVisibility.isVisible(tab)
                    val switchEnabled = !isVisible || visibleCount > 1
                    
                    BottomNavConfigCard(
                        icon = tab.toIcon(),
                        title = tab.toLabel(),
                        subtitle = if (isVisible) "已显示" else "已隐藏",
                        checked = isVisible,
                        switchEnabled = switchEnabled,
                        canMoveUp = index > 0,
                        canMoveDown = index < bottomNavOrder.lastIndex,
                        onCheckedChange = { checked ->
                            viewModel.updateBottomNavVisibility(tab, checked)
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newOrder = bottomNavOrder.toMutableList().apply {
                                    add(index - 1, removeAt(index))
                                }
                                viewModel.updateBottomNavOrder(newOrder)
                            }
                        },
                        onMoveDown = {
                            if (index < bottomNavOrder.lastIndex) {
                                val newOrder = bottomNavOrder.toMutableList().apply {
                                    add(index + 1, removeAt(index))
                                }
                                viewModel.updateBottomNavOrder(newOrder)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavConfigCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    switchEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 标题和副标题
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 上移按钮
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "上移",
                    tint = if (canMoveUp) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            
            // 下移按钮
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "下移",
                    tint = if (canMoveDown) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            
            // 开关
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = switchEnabled
            )
        }
    }
}

// 扩展函数：将 BottomNavTab 转换为图标
private fun takagi.ru.saison.data.local.datastore.BottomNavTab.toIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    takagi.ru.saison.data.local.datastore.BottomNavTab.COURSE -> Icons.Default.School
    takagi.ru.saison.data.local.datastore.BottomNavTab.CALENDAR -> Icons.Default.CalendarToday
    takagi.ru.saison.data.local.datastore.BottomNavTab.TASKS -> Icons.Default.CheckCircle
    takagi.ru.saison.data.local.datastore.BottomNavTab.POMODORO -> Icons.Default.Timer
    takagi.ru.saison.data.local.datastore.BottomNavTab.SUBSCRIPTION -> Icons.Default.Star
    takagi.ru.saison.data.local.datastore.BottomNavTab.SETTINGS -> Icons.Default.Settings
}

// 扩展函数：将 BottomNavTab 转换为标签
private fun takagi.ru.saison.data.local.datastore.BottomNavTab.toLabel(): String = when (this) {
    takagi.ru.saison.data.local.datastore.BottomNavTab.COURSE -> "课程"
    takagi.ru.saison.data.local.datastore.BottomNavTab.CALENDAR -> "日历"
    takagi.ru.saison.data.local.datastore.BottomNavTab.TASKS -> "任务"
    takagi.ru.saison.data.local.datastore.BottomNavTab.POMODORO -> "专注"
    takagi.ru.saison.data.local.datastore.BottomNavTab.SUBSCRIPTION -> "订阅"
    takagi.ru.saison.data.local.datastore.BottomNavTab.SETTINGS -> "设置"
}


/**
 * ThemeModeOptionCard - 主题模式选项卡片
 * 
 * 显示单个主题模式选项，包括 RadioButton、名称和描述
 */
@Composable
private fun ThemeModeOptionCard(
    mode: takagi.ru.saison.data.local.datastore.ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeName = stringResource(mode.displayNameRes)
    val stateText = if (isSelected) "已选中" else "未选中"
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.semantics {
                    contentDescription = "$modeName, $stateText"
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(mode.displayNameRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * ThemeModeSelectionDialog - 主题模式选择对话框
 * 
 * 显示三个主题模式选项供用户选择
 */
@Composable
fun ThemeModeSelectionDialog(
    currentMode: takagi.ru.saison.data.local.datastore.ThemeMode,
    onModeSelected: (takagi.ru.saison.data.local.datastore.ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_theme_mode_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                takagi.ru.saison.data.local.datastore.ThemeMode.values().forEach { mode ->
                    ThemeModeOptionCard(
                        mode = mode,
                        isSelected = mode == currentMode,
                        onClick = {
                            onModeSelected(mode)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_action_close))
            }
        },
        modifier = modifier
    )
}

/**
 * 获取主题模式的显示名称
 */
@Composable
fun getThemeModeName(mode: takagi.ru.saison.data.local.datastore.ThemeMode): String {
    return stringResource(mode.displayNameRes)
}


/**
 * ExportOptionsDialog - 导出选项对话框
 * 
 * 允许用户选择导出范围和格式
 */
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit,
    isExporting: Boolean = false,
    modifier: Modifier = Modifier
) {
    var exportMode by remember { mutableStateOf(ExportMode.CURRENT_SEMESTER) }
    var compatibilityMode by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.export_options_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 导出范围选择
                Text(
                    text = stringResource(R.string.export_scope),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButtonOption(
                        selected = exportMode == ExportMode.CURRENT_SEMESTER,
                        onClick = { exportMode = ExportMode.CURRENT_SEMESTER },
                        text = stringResource(R.string.export_current_semester)
                    )
                    
                    RadioButtonOption(
                        selected = exportMode == ExportMode.ALL_SEMESTERS,
                        onClick = { exportMode = ExportMode.ALL_SEMESTERS },
                        text = stringResource(R.string.export_all_semesters)
                    )
                }
                
                Divider()
                
                // 导出格式选择
                Text(
                    text = stringResource(R.string.export_format),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { compatibilityMode = !compatibilityMode }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.include_full_config),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.include_full_config_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = !compatibilityMode,
                            onCheckedChange = { compatibilityMode = !it }
                        )
                    }
                }
                
                if (compatibilityMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.compatibility_mode_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(
                        ExportOptions(
                            mode = exportMode,
                            compatibilityMode = compatibilityMode
                        )
                    )
                },
                enabled = !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isExporting) 
                        stringResource(R.string.export_in_progress) 
                    else 
                        stringResource(R.string.export_button)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * RadioButtonOption - 单选按钮选项
 */
@Composable
private fun RadioButtonOption(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
