package takagi.ru.saison.ui.screens.settings.local

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.ui.components.local.ExportActionsCard
import takagi.ru.saison.ui.components.local.ExportPreferencesCard
import takagi.ru.saison.ui.components.local.ImportActionsCard
import takagi.ru.saison.ui.components.local.ImportPreviewDialog
import takagi.ru.saison.ui.components.local.ImportSummaryDialog

/**
 * 本地导出导入屏幕
 * 提供本地文件导出导入功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalExportImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocalExportImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 选项卡状态
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // 文件选择器 Launchers
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        viewModel.handleExportZipResult(uri)
    }
    
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        viewModel.handleExportJsonResult(uri)
    }
    
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.handleImportZipResult(uri)
    }
    
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.handleImportJsonResult(uri)
    }
    
    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
    
    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }
    
    // 数据类型选择对话框
    if (uiState.showDataTypeSelector) {
        DataTypeSelectorDialog(
            dataCounts = uiState.dataCounts,
            onDataTypeSelected = { dataType ->
                viewModel.selectDataTypeForExport(dataType)
                viewModel.hideDataTypeSelector()
                // 生成文件名并启动文件选择器
                val dataTypeName = dataType.fileName.removeSuffix(".json")
                val fileName = "saison_${dataTypeName}_${generateTimestamp()}.json"
                exportJsonLauncher.launch(fileName)
            },
            onDismiss = { viewModel.hideDataTypeSelector() }
        )
    }
    
    // 导入预览对话框
    if (uiState.showPreviewDialog && uiState.importPreview != null) {
        ImportPreviewDialog(
            preview = uiState.importPreview!!,
            onConfirm = { viewModel.confirmImport() },
            onDismiss = { viewModel.cancelImport() }
        )
    }
    
    // 导入摘要对话框
    if (uiState.importSummary != null) {
        ImportSummaryDialog(
            summary = uiState.importSummary!!,
            onDismiss = { viewModel.clearImportSummary() }
        )
    }
    
    // 进度对话框
    if (uiState.isExporting || uiState.isImporting || uiState.isLoadingPreview) {
        ProgressDialog(
            message = when {
                uiState.isExporting -> stringResource(R.string.progress_exporting)
                uiState.isImporting -> stringResource(R.string.progress_importing)
                uiState.isLoadingPreview -> stringResource(R.string.progress_loading)
                else -> ""
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.local_export_import_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 选项卡
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.local_export_tab)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.local_import_tab)) }
                )
            }
            
            // 内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // 导出选项卡
                        ExportPreferencesCard(
                            preferences = uiState.exportPreferences,
                            dataCounts = uiState.dataCounts,
                            onToggleDataType = { dataType, enabled ->
                                viewModel.toggleDataType(dataType, enabled)
                            },
                            onSelectAll = { viewModel.selectAllDataTypes() },
                            onDeselectAll = { viewModel.deselectAllDataTypes() }
                        )
                        
                        ExportActionsCard(
                            onExportZip = {
                                val fileName = "saison_backup_${generateTimestamp()}.zip"
                                exportZipLauncher.launch(fileName)
                            },
                            onExportJson = {
                                viewModel.showDataTypeSelectorForExport()
                            },
                            isExporting = uiState.isExporting
                        )
                    }
                    1 -> {
                        // 导入选项卡
                        ImportActionsCard(
                            onImportZip = {
                                importZipLauncher.launch(arrayOf("application/zip"))
                            },
                            onImportJson = {
                                importJsonLauncher.launch(arrayOf("application/json"))
                            },
                            isImporting = uiState.isImporting || uiState.isLoadingPreview
                        )
                        
                        // 导入说明
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.local_import_info_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.local_import_info_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 数据类型选择对话框
 */
@Composable
private fun DataTypeSelectorDialog(
    dataCounts: Map<DataType, Int>,
    onDataTypeSelected: (DataType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.local_export_select_data_type))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataType.values().forEach { dataType ->
                    val count = dataCounts[dataType] ?: 0
                    Surface(
                        onClick = { onDataTypeSelected(dataType) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(getDataTypeNameRes(dataType)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 获取数据类型的名称资源 ID
 */
private fun getDataTypeNameRes(dataType: DataType): Int {
    return when (dataType) {
        DataType.TASKS -> R.string.local_export_data_type_tasks
        DataType.COURSES -> R.string.local_export_data_type_courses
        DataType.EVENTS -> R.string.local_export_data_type_events
        DataType.ROUTINES -> R.string.local_export_data_type_routines
        DataType.SUBSCRIPTIONS -> R.string.local_export_data_type_subscriptions
        DataType.POMODORO_SESSIONS -> R.string.local_export_data_type_pomodoros
        DataType.SEMESTERS -> R.string.local_export_data_type_semesters
        DataType.PREFERENCES -> R.string.local_export_data_type_preferences
    }
}

/**
 * 生成时间戳字符串
 */
private fun generateTimestamp(): String {
    val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}


/**
 * 进度对话框
 * 显示操作进行中的状态
 */
@Composable
private fun ProgressDialog(
    message: String
) {
    AlertDialog(
        onDismissRequest = { /* 不允许取消 */ },
        title = {
            Text(
                text = stringResource(R.string.progress_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {}
    )
}
