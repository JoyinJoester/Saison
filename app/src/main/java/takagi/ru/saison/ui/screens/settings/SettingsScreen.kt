package takagi.ru.saison.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import takagi.ru.saison.R
import takagi.ru.saison.ui.screens.settings.components.SettingsItem
import takagi.ru.saison.ui.screens.settings.components.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToWebDavBackup: () -> Unit = {},
    onNavigateToLocalExportImport: () -> Unit = {},
    onNavigateToSaisonPlus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val isPlusActivated by viewModel.isPlusActivated.collectAsState()

    var showThemeBottomSheet by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showBottomNavBottomSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
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

    val confirmText = stringResource(R.string.common_action_confirm)
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
                    android.util.Log.d("SettingsScreen", "RestartRequired event received")
                    val activity = context as? android.app.Activity
                    android.util.Log.d("SettingsScreen", "Activity: $activity")
                    activity?.recreate()
                    android.util.Log.d("SettingsScreen", "recreate() called")
                }
                is SettingsUiEvent.NavigateToSystemSettings -> {
                    // TODO: 导航到系统设置
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
            takagi.ru.saison.ui.components.SaisonPlusCard(
                isPlusActivated = isPlusActivated,
                onClick = onNavigateToSaisonPlus
            )

            SettingsSection(title = stringResource(R.string.settings_section_data_sync)) {
                SettingsItem(
                    icon = Icons.Default.ImportExport,
                    title = stringResource(R.string.local_export_import_title),
                    subtitle = stringResource(R.string.local_export_import_subtitle),
                    onClick = onNavigateToLocalExportImport
                )

                if (isPlusActivated) {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "WebDAV 备份",
                        subtitle = "配置和管理 WebDAV 备份",
                        onClick = onNavigateToWebDavBackup
                    )
                }
            }

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

            SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = if (notificationsEnabled) {
                        stringResource(R.string.settings_notifications_enabled)
                    } else {
                        stringResource(R.string.settings_notifications_disabled)
                    },
                    onClick = onNavigateToNotificationSettings
                )
            }

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

    if (showThemeBottomSheet) {
        ThemeBottomSheet(
            currentTheme = currentTheme,
            isPlusActivated = isPlusActivated,
            onThemeSelected = { theme ->
                viewModel.setTheme(theme)
            },
            onNavigateToPlus = onNavigateToSaisonPlus,
            onDismiss = { showThemeBottomSheet = false }
        )
    }

    if (showThemeModeDialog) {
        ThemeModeSelectionDialog(
            currentMode = themeMode,
            onModeSelected = { mode ->
                viewModel.setThemeMode(mode)
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    if (showBottomNavBottomSheet) {
        BottomNavBottomSheet(
            viewModel = viewModel,
            onDismiss = { showBottomNavBottomSheet = false }
        )
    }

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

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    val showExportDialog by viewModel.showExportDialog.collectAsState()
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
                    viewModel.saveExportOptions(options.semesterIds)

                    val fileName = if (options.semesterIds.size == 1) {
                        viewModel.getSuggestedFileName(options.semesterIds.first())
                    } else {
                        "课程表_${System.currentTimeMillis()}.json"
                    }

                    exportLauncher.launch(fileName)
                }
            }
        )
    }
}
