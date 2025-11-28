package takagi.ru.saison.data.repository.local

import android.net.Uri
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.backup.ExportSummary
import takagi.ru.saison.domain.model.backup.ImportPreview
import takagi.ru.saison.domain.model.backup.RestoreSummary

/**
 * 本地导出导入 Repository 接口
 */
interface LocalExportImportRepository {
    
    /**
     * 导出为 ZIP 文件
     * @param uri 用户选择的保存位置 URI
     * @param preferences 导出偏好设置
     * @return 导出摘要
     */
    suspend fun exportToZip(
        uri: Uri,
        preferences: BackupPreferences
    ): Result<ExportSummary>
    
    /**
     * 导出为单个 JSON 文件
     * @param uri 用户选择的保存位置 URI
     * @param dataType 要导出的数据类型
     * @return 导出摘要
     */
    suspend fun exportToJson(
        uri: Uri,
        dataType: DataType
    ): Result<ExportSummary>
    
    /**
     * 从 ZIP 文件导入
     * @param uri 用户选择的 ZIP 文件 URI
     * @return 恢复摘要
     */
    suspend fun importFromZip(uri: Uri): Result<RestoreSummary>
    
    /**
     * 从 JSON 文件导入
     * @param uri 用户选择的 JSON 文件 URI
     * @param dataType 数据类型（如果为 null 则自动检测）
     * @return 恢复摘要
     */
    suspend fun importFromJson(
        uri: Uri,
        dataType: DataType? = null
    ): Result<RestoreSummary>
    
    /**
     * 预览导入内容
     * @param uri 文件 URI
     * @return 导入预览信息
     */
    suspend fun previewImport(uri: Uri): Result<ImportPreview>
    
    /**
     * 保存导出偏好设置
     */
    suspend fun saveExportPreferences(preferences: BackupPreferences)
    
    /**
     * 获取导出偏好设置
     */
    suspend fun getExportPreferences(): BackupPreferences
    
    /**
     * 获取各数据类型的项目数量
     */
    suspend fun getDataCounts(): Map<DataType, Int>
}
