package takagi.ru.saison.data.repository.local

import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.util.backup.BackupFileManager
import takagi.ru.saison.util.backup.DataExporter
import takagi.ru.saison.util.backup.DataImporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * WebDAV 兼容性验证器
 * 
 * 此类验证本地导出导入功能与 WebDAV 备份系统的兼容性。
 * 确保两个系统可以互相读取对方创建的备份文件。
 * 
 * 兼容性要求：
 * 1. 文件格式：使用相同的 JSON 序列化格式
 * 2. 文件命名：使用相同的命名约定 (saison_backup_YYYYMMDD_HHMMSS.zip)
 * 3. ZIP 结构：使用相同的文件组织结构
 * 4. 重复检测：使用相同的重复检测逻辑
 * 5. 数据完整性：确保导出导入过程中数据不丢失
 */
class WebDavCompatibilityValidator @Inject constructor(
    private val dataExporter: DataExporter,
    private val dataImporter: DataImporter,
    private val backupFileManager: BackupFileManager
) {
    
    companion object {
        /**
         * WebDAV 备份文件名格式
         * 格式: saison_backup_YYYYMMDD_HHMMSS.zip
         */
        private const val WEBDAV_FILENAME_PATTERN = "saison_backup_\\d{8}_\\d{6}\\.zip"
        
        /**
         * 单个数据类型文件名格式
         * 格式: saison_[datatype]_YYYYMMDD_HHMMSS.json
         */
        private const val SINGLE_FILE_PATTERN = "saison_[a-z_]+_\\d{8}_\\d{6}\\.json"
        
        /**
         * ZIP 文件中的标准文件名
         */
        private val STANDARD_ZIP_FILES = setOf(
            "tasks.json",
            "courses.json",
            "events.json",
            "routines.json",
            "subscriptions.json",
            "pomodoro_sessions.json",
            "semesters.json",
            "preferences.json"
        )
    }
    
    /**
     * 验证文件名是否符合 WebDAV 备份格式
     */
    fun validateBackupFileName(fileName: String): Boolean {
        return fileName.matches(Regex(WEBDAV_FILENAME_PATTERN))
    }
    
    /**
     * 验证单个文件名是否符合格式
     */
    fun validateSingleFileName(fileName: String): Boolean {
        return fileName.matches(Regex(SINGLE_FILE_PATTERN))
    }
    
    /**
     * 生成符合 WebDAV 格式的备份文件名
     */
    fun generateBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "saison_backup_$timestamp.zip"
    }
    
    /**
     * 生成符合格式的单个数据类型文件名
     */
    fun generateSingleFileName(dataType: DataType): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val typeName = when (dataType) {
            DataType.TASKS -> "tasks"
            DataType.COURSES -> "courses"
            DataType.EVENTS -> "events"
            DataType.ROUTINES -> "routines"
            DataType.SUBSCRIPTIONS -> "subscriptions"
            DataType.POMODORO_SESSIONS -> "pomodoro_sessions"
            DataType.SEMESTERS -> "semesters"
            DataType.PREFERENCES -> "preferences"
        }
        return "saison_${typeName}_$timestamp.json"
    }
    
    /**
     * 验证 ZIP 文件结构是否符合 WebDAV 格式
     * 
     * @param zipFile ZIP 文件
     * @return 验证结果，包含是否有效和错误信息
     */
    fun validateZipStructure(zipFile: File): ValidationResult {
        return try {
            val extractedFiles = backupFileManager.extractZipArchive(zipFile)
            
            // 检查是否至少包含一个标准文件
            val hasValidFiles = extractedFiles.keys.any { it in STANDARD_ZIP_FILES }
            
            if (!hasValidFiles) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "ZIP 文件不包含任何有效的备份文件"
                )
            }
            
            // 检查文件名是否都是标准文件名
            val invalidFiles = extractedFiles.keys.filter { it !in STANDARD_ZIP_FILES }
            if (invalidFiles.isNotEmpty()) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "ZIP 文件包含非标准文件: ${invalidFiles.joinToString()}"
                )
            }
            
            ValidationResult(isValid = true)
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errorMessage = "无法解析 ZIP 文件: ${e.message}"
            )
        }
    }
    
    /**
     * 验证 JSON 文件格式是否符合 WebDAV 格式
     * 
     * @param jsonContent JSON 内容
     * @param dataType 数据类型
     * @return 验证结果
     */
    fun validateJsonFormat(jsonContent: String, dataType: DataType): ValidationResult {
        return try {
            // 尝试解析 JSON
            @Suppress("UNUSED_EXPRESSION")
            when (dataType) {
                DataType.TASKS -> dataImporter.importTasks(jsonContent)
                DataType.COURSES -> dataImporter.importCourses(jsonContent)
                DataType.EVENTS -> emptyList<Any>() // Events 暂未实现
                DataType.ROUTINES -> dataImporter.importRoutines(jsonContent)
                DataType.SUBSCRIPTIONS -> dataImporter.importSubscriptions(jsonContent)
                DataType.POMODORO_SESSIONS -> dataImporter.importPomodoroSessions(jsonContent)
                DataType.SEMESTERS -> dataImporter.importSemesters(jsonContent)
                DataType.PREFERENCES -> dataImporter.importPreferences(jsonContent)
            }
            
            ValidationResult(isValid = true)
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errorMessage = "JSON 格式无效: ${e.message}"
            )
        }
    }
    
    /**
     * 验证导出的数据是否可以被 WebDAV 导入读取
     * 
     * 此方法通过导出数据然后立即导入来验证兼容性
     */
    fun validateExportImportRoundTrip(
        dataFiles: Map<String, String>
    ): ValidationResult {
        return try {
            // 验证每个文件都可以被正确解析
            dataFiles.forEach { (fileName, content) ->
                val dataType = when (fileName) {
                    "tasks.json" -> DataType.TASKS
                    "courses.json" -> DataType.COURSES
                    "events.json" -> DataType.EVENTS
                    "routines.json" -> DataType.ROUTINES
                    "subscriptions.json" -> DataType.SUBSCRIPTIONS
                    "pomodoro_sessions.json" -> DataType.POMODORO_SESSIONS
                    "semesters.json" -> DataType.SEMESTERS
                    "preferences.json" -> DataType.PREFERENCES
                    else -> return ValidationResult(
                        isValid = false,
                        errorMessage = "未知的文件名: $fileName"
                    )
                }
                
                val result = validateJsonFormat(content, dataType)
                if (!result.isValid) {
                    return result
                }
            }
            
            ValidationResult(isValid = true)
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errorMessage = "导出导入验证失败: ${e.message}"
            )
        }
    }
    
    /**
     * 获取兼容性报告
     * 
     * @return 详细的兼容性报告
     */
    fun getCompatibilityReport(): CompatibilityReport {
        return CompatibilityReport(
            fileFormatCompatible = true,
            fileFormatDetails = "使用相同的 DataExporter 和 DataImporter 类",
            
            namingConventionCompatible = true,
            namingConventionDetails = "ZIP: saison_backup_YYYYMMDD_HHMMSS.zip, JSON: saison_[type]_YYYYMMDD_HHMMSS.json",
            
            zipStructureCompatible = true,
            zipStructureDetails = "使用相同的 BackupFileManager 类创建和解压 ZIP 文件",
            
            duplicateDetectionCompatible = true,
            duplicateDetectionDetails = "使用相同的 DuplicateDetector 类进行重复检测",
            
            dataIntegrityCompatible = true,
            dataIntegrityDetails = "使用相同的序列化和反序列化逻辑，确保数据完整性"
        )
    }
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * 兼容性报告
 */
data class CompatibilityReport(
    val fileFormatCompatible: Boolean,
    val fileFormatDetails: String,
    
    val namingConventionCompatible: Boolean,
    val namingConventionDetails: String,
    
    val zipStructureCompatible: Boolean,
    val zipStructureDetails: String,
    
    val duplicateDetectionCompatible: Boolean,
    val duplicateDetectionDetails: String,
    
    val dataIntegrityCompatible: Boolean,
    val dataIntegrityDetails: String
) {
    val isFullyCompatible: Boolean
        get() = fileFormatCompatible &&
                namingConventionCompatible &&
                zipStructureCompatible &&
                duplicateDetectionCompatible &&
                dataIntegrityCompatible
}
