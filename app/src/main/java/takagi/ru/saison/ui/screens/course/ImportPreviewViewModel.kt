package takagi.ru.saison.ui.screens.course

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import takagi.ru.saison.domain.model.courseexport.SemesterExportData
import takagi.ru.saison.domain.usecase.ConflictInfo
import takagi.ru.saison.domain.usecase.ImportCourseDataUseCase
import takagi.ru.saison.domain.usecase.ImportOptions
import javax.inject.Inject

/**
 * 导入预览界面的ViewModel
 * 管理导入数据的加载、验证和导入执行
 */
@HiltViewModel
class ImportPreviewViewModel @Inject constructor(
    private val importUseCase: ImportCourseDataUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val importUri: String? = savedStateHandle["uri"]
    
    private val _uiState = MutableStateFlow<ImportPreviewUiState>(ImportPreviewUiState.Loading)
    val uiState: StateFlow<ImportPreviewUiState> = _uiState.asStateFlow()
    
    private val _importSuccessEvent = Channel<Long>(Channel.BUFFERED)
    val importSuccessEvent = _importSuccessEvent.receiveAsFlow()
    
    private var currentData: SemesterExportData? = null
    
    init {
        loadImportData()
    }
    
    private fun loadImportData() {
        viewModelScope.launch {
            _uiState.value = ImportPreviewUiState.Loading
            
            val uri = importUri?.let { Uri.parse(it) }
            if (uri == null) {
                _uiState.value = ImportPreviewUiState.Error("无效的文件路径")
                return@launch
            }
            
            // 解析文件
            val parseResult = importUseCase.parseFromUri(uri)
            if (parseResult.isFailure) {
                _uiState.value = ImportPreviewUiState.Error(
                    parseResult.exceptionOrNull()?.message ?: "解析失败"
                )
                return@launch
            }
            
            val exportData = parseResult.getOrThrow()
            
            // 目前只支持导入第一个学期
            val semesterData = exportData.semesters.firstOrNull()
            if (semesterData == null) {
                _uiState.value = ImportPreviewUiState.Error("文件中没有学期数据")
                return@launch
            }
            
            currentData = semesterData
            
            // 检测冲突
            val conflicts = importUseCase.detectConflicts(semesterData)
            
            // 生成默认学期名称（如果有冲突则添加后缀）
            val defaultName = if (conflicts.hasNameConflict) {
                "${semesterData.semesterInfo.name} (导入)"
            } else {
                semesterData.semesterInfo.name
            }
            
            _uiState.value = ImportPreviewUiState.Success(
                data = semesterData,
                conflicts = conflicts,
                semesterName = defaultName,
                applyPeriodSettings = conflicts.hasPeriodSettingsConflict,
                applyDisplaySettings = conflicts.hasDisplaySettingsConflict,
                isImporting = false
            )
        }
    }
    
    fun updateSemesterName(name: String) {
        val currentState = _uiState.value
        if (currentState is ImportPreviewUiState.Success) {
            _uiState.value = currentState.copy(semesterName = name)
        }
    }
    
    fun updateApplyPeriodSettings(apply: Boolean) {
        val currentState = _uiState.value
        if (currentState is ImportPreviewUiState.Success) {
            _uiState.value = currentState.copy(applyPeriodSettings = apply)
        }
    }
    
    fun updateApplyDisplaySettings(apply: Boolean) {
        val currentState = _uiState.value
        if (currentState is ImportPreviewUiState.Success) {
            _uiState.value = currentState.copy(applyDisplaySettings = apply)
        }
    }
    
    fun executeImport() {
        val currentState = _uiState.value
        if (currentState !is ImportPreviewUiState.Success || currentState.isImporting) {
            return
        }
        
        val data = currentData ?: return
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isImporting = true)
            
            val options = ImportOptions(
                semesterName = currentState.semesterName,
                applyPeriodSettings = currentState.applyPeriodSettings,
                applyDisplaySettings = currentState.applyDisplaySettings
            )
            
            val result = importUseCase.executeImport(data, options)
            
            if (result.isSuccess) {
                val importResult = result.getOrThrow()
                _importSuccessEvent.send(importResult.semesterId)
            } else {
                _uiState.value = ImportPreviewUiState.Error(
                    result.exceptionOrNull()?.message ?: "导入失败"
                )
            }
        }
    }
    
    fun retry() {
        loadImportData()
    }
}

/**
 * 导入预览界面的UI状态
 */
sealed class ImportPreviewUiState {
    object Loading : ImportPreviewUiState()
    
    data class Error(val message: String) : ImportPreviewUiState()
    
    data class Success(
        val data: SemesterExportData,
        val conflicts: ConflictInfo,
        val semesterName: String,
        val applyPeriodSettings: Boolean,
        val applyDisplaySettings: Boolean,
        val isImporting: Boolean
    ) : ImportPreviewUiState()
}
