package takagi.ru.saison.ui.screens.settings.local

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.backup.ExportSummary
import takagi.ru.saison.domain.model.backup.ImportPreview
import takagi.ru.saison.domain.model.backup.RestoreSummary
import takagi.ru.saison.domain.usecase.local.ExportToJsonUseCase
import takagi.ru.saison.domain.usecase.local.ExportToZipUseCase
import takagi.ru.saison.domain.usecase.local.ImportFromJsonUseCase
import takagi.ru.saison.domain.usecase.local.ImportFromZipUseCase
import takagi.ru.saison.domain.usecase.local.PreviewImportUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 本地导出导入屏幕的 UI 状态
 */
data class LocalExportImportUiState(
    // 导出偏好设置
    val exportPreferences: BackupPreferences = BackupPreferences(),
    
    // 数据计数
    val dataCounts: Map<DataType, Int> = emptyMap(),
    val isLoadingCounts: Boolean = false,
    
    // 导出状态
    val isExporting: Boolean = false,
    val exportSummary: ExportSummary? = null,
    
    // 导入状态
    val isImporting: Boolean = false,
    val importSummary: RestoreSummary? = null,
    
    // 导入预览
    val importPreview: ImportPreview? = null,
    val isLoadingPreview: Boolean = false,
    val showPreviewDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
    val pendingImportIsZip: Boolean = false,
    
    // 单个 JSON 导出选择
    val selectedDataTypeForExport: DataType? = null,
    val showDataTypeSelector: Boolean = false,
    
    // 错误和成功消息
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * 本地导出导入 ViewModel
 */
@HiltViewModel
class LocalExportImportViewModel @Inject constructor(
    private val repository: LocalExportImportRepository,
    private val exportToZipUseCase: ExportToZipUseCase,
    private val exportToJsonUseCase: ExportToJsonUseCase,
    private val importFromZipUseCase: ImportFromZipUseCase,
    private val importFromJsonUseCase: ImportFromJsonUseCase,
    private val previewImportUseCase: PreviewImportUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LocalExportImportUiState())
    val uiState: StateFlow<LocalExportImportUiState> = _uiState.asStateFlow()
    
    init {
        loadExportPreferences()
        loadDataCounts()
    }
    
    /**
     * 加载导出偏好设置
     */
    private fun loadExportPreferences() {
        viewModelScope.launch {
            try {
                val preferences = repository.getExportPreferences()
                _uiState.update { it.copy(exportPreferences = preferences) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "加载偏好设置失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 加载数据计数
     */
    fun loadDataCounts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingCounts = true) }
                val counts = repository.getDataCounts()
                _uiState.update {
                    it.copy(
                        dataCounts = counts,
                        isLoadingCounts = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCounts = false,
                        error = "加载数据计数失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 更新导出偏好设置
     */
    fun updateExportPreferences(preferences: BackupPreferences) {
        viewModelScope.launch {
            try {
                repository.saveExportPreferences(preferences)
                _uiState.update { it.copy(exportPreferences = preferences) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存偏好设置失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 创建导出 ZIP 的 SAF Intent
     * Requirements: 1.1
     */
    fun createExportZipIntent(): Intent {
        val timestamp = generateTimestamp()
        val fileName = "saison_backup_$timestamp.zip"
        
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }
    
    /**
     * 创建导出 JSON 的 SAF Intent
     * Requirements: 2.2
     */
    fun createExportJsonIntent(dataType: DataType): Intent {
        val timestamp = generateTimestamp()
        val dataTypeName = dataType.fileName.removeSuffix(".json")
        val fileName = "saison_${dataTypeName}_$timestamp.json"
        
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }
    
    /**
     * 创建导入 ZIP 的 SAF Intent
     * Requirements: 3.1
     */
    fun createImportZipIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
    }
    
    /**
     * 创建导入 JSON 的 SAF Intent
     * Requirements: 4.1
     */
    fun createImportJsonIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
    }
    
    /**
     * 生成时间戳字符串
     */
    private fun generateTimestamp(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }
    
    /**
     * 切换数据类型的导出状态
     */
    fun toggleDataType(dataType: DataType, enabled: Boolean) {
        val currentPreferences = _uiState.value.exportPreferences
        val updatedPreferences = when (dataType) {
            DataType.TASKS -> currentPreferences.copy(includeTasks = enabled)
            DataType.COURSES -> currentPreferences.copy(includeCourses = enabled)
            DataType.EVENTS -> currentPreferences.copy(includeEvents = enabled)
            DataType.ROUTINES -> currentPreferences.copy(includeRoutines = enabled)
            DataType.SUBSCRIPTIONS -> currentPreferences.copy(includeSubscriptions = enabled)
            DataType.POMODORO_SESSIONS -> currentPreferences.copy(includePomodoroSessions = enabled)
            DataType.SEMESTERS -> currentPreferences.copy(includeSemesters = enabled)
            DataType.PREFERENCES -> currentPreferences.copy(includePreferences = enabled)
        }
        updateExportPreferences(updatedPreferences)
    }
    
    /**
     * 全选所有数据类型
     */
    fun selectAllDataTypes() {
        val updatedPreferences = BackupPreferences(
            includeTasks = true,
            includeCourses = true,
            includeEvents = true,
            includeRoutines = true,
            includeSubscriptions = true,
            includePomodoroSessions = true,
            includeSemesters = true,
            includePreferences = true
        )
        updateExportPreferences(updatedPreferences)
    }
    
    /**
     * 取消全选所有数据类型
     */
    fun deselectAllDataTypes() {
        val updatedPreferences = BackupPreferences(
            includeTasks = false,
            includeCourses = false,
            includeEvents = false,
            includeRoutines = false,
            includeSubscriptions = false,
            includePomodoroSessions = false,
            includeSemesters = false,
            includePreferences = false
        )
        updateExportPreferences(updatedPreferences)
    }
    
    /**
     * 处理导出 ZIP 的 Activity Result
     * Requirements: 1.1
     */
    fun handleExportZipResult(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(error = "未选择文件") }
            return
        }
        exportToZip(uri)
    }
    
    /**
     * 导出为 ZIP 文件
     */
    private fun exportToZip(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null, exportSummary = null) }
                
                val preferences = _uiState.value.exportPreferences
                val result = exportToZipUseCase(uri, preferences)
                
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSummary = summary,
                            successMessage = "导出成功，共导出 ${summary?.totalItems ?: 0} 项"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            error = "导出失败: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = "导出异常: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 显示数据类型选择器（用于单个 JSON 导出）
     */
    fun showDataTypeSelectorForExport() {
        _uiState.update { it.copy(showDataTypeSelector = true) }
    }
    
    /**
     * 隐藏数据类型选择器
     */
    fun hideDataTypeSelector() {
        _uiState.update {
            it.copy(
                showDataTypeSelector = false,
                selectedDataTypeForExport = null
            )
        }
    }
    
    /**
     * 选择要导出的数据类型
     */
    fun selectDataTypeForExport(dataType: DataType) {
        _uiState.update { it.copy(selectedDataTypeForExport = dataType) }
    }
    
    /**
     * 处理导出 JSON 的 Activity Result
     * Requirements: 2.2
     */
    fun handleExportJsonResult(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(error = "未选择文件") }
            return
        }
        
        val dataType = _uiState.value.selectedDataTypeForExport
        if (dataType == null) {
            _uiState.update { it.copy(error = "未选择数据类型") }
            return
        }
        
        exportToJson(uri, dataType)
    }
    
    /**
     * 导出为单个 JSON 文件
     */
    private fun exportToJson(uri: Uri, dataType: DataType) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, error = null, exportSummary = null) }
                
                val result = exportToJsonUseCase(uri, dataType)
                
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportSummary = summary,
                            successMessage = "导出成功，共导出 ${summary?.totalItems ?: 0} 项",
                            showDataTypeSelector = false,
                            selectedDataTypeForExport = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            error = "导出失败: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = "导出异常: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * 处理导入 ZIP 的 Activity Result
     * Requirements: 3.1
     */
    fun handleImportZipResult(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(error = "未选择文件") }
            return
        }
        previewImport(uri, isZip = true)
    }
    
    /**
     * 处理导入 JSON 的 Activity Result
     * Requirements: 4.1
     */
    fun handleImportJsonResult(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(error = "未选择文件") }
            return
        }
        previewImport(uri, isZip = false)
    }
    
    /**
     * 预览导入内容
     */
    private fun previewImport(uri: Uri, isZip: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoadingPreview = true,
                        error = null,
                        importPreview = null,
                        pendingImportUri = uri,
                        pendingImportIsZip = isZip
                    )
                }
                
                val result = previewImportUseCase(uri)
                
                if (result.isSuccess) {
                    val preview = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            importPreview = preview,
                            showPreviewDialog = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            error = "预览失败: ${result.exceptionOrNull()?.message}",
                            pendingImportUri = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPreview = false,
                        error = "预览异常: ${e.message}",
                        pendingImportUri = null
                    )
                }
            }
        }
    }
    
    /**
     * 确认导入
     */
    fun confirmImport() {
        val uri = _uiState.value.pendingImportUri ?: return
        val isZip = _uiState.value.pendingImportIsZip
        
        _uiState.update {
            it.copy(
                showPreviewDialog = false,
                importPreview = null
            )
        }
        
        if (isZip) {
            importFromZip(uri)
        } else {
            importFromJson(uri)
        }
    }
    
    /**
     * 取消导入
     */
    fun cancelImport() {
        _uiState.update {
            it.copy(
                showPreviewDialog = false,
                importPreview = null,
                pendingImportUri = null,
                pendingImportIsZip = false
            )
        }
    }
    
    /**
     * 从 ZIP 文件导入
     */
    private fun importFromZip(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isImporting = true, error = null, importSummary = null) }
                
                val result = importFromZipUseCase(uri)
                
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSummary = summary,
                            successMessage = "导入成功，共导入 ${summary?.totalImported ?: 0} 项",
                            pendingImportUri = null
                        )
                    }
                    // 重新加载数据计数
                    loadDataCounts()
                } else {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = "导入失败: ${result.exceptionOrNull()?.message}",
                            pendingImportUri = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "导入异常: ${e.message}",
                        pendingImportUri = null
                    )
                }
            }
        }
    }
    
    /**
     * 从 JSON 文件导入
     */
    private fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isImporting = true, error = null, importSummary = null) }
                
                // 自动检测数据类型
                val result = importFromJsonUseCase(uri, null)
                
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSummary = summary,
                            successMessage = "导入成功，共导入 ${summary?.totalImported ?: 0} 项",
                            pendingImportUri = null
                        )
                    }
                    // 重新加载数据计数
                    loadDataCounts()
                } else {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = "导入失败: ${result.exceptionOrNull()?.message}",
                            pendingImportUri = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "导入异常: ${e.message}",
                        pendingImportUri = null
                    )
                }
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    /**
     * 清除导出摘要
     */
    fun clearExportSummary() {
        _uiState.update { it.copy(exportSummary = null) }
    }
    
    /**
     * 清除导入摘要
     */
    fun clearImportSummary() {
        _uiState.update { it.copy(importSummary = null) }
    }
}
