package takagi.ru.saison.data.repository.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import takagi.ru.saison.data.local.webdav.WebDavConfigStorage
import takagi.ru.saison.data.local.webdav.WebDavPathManager
import takagi.ru.saison.data.remote.webdav.WebDavClient
import takagi.ru.saison.data.remote.webdav.WebDavCredentials
import takagi.ru.saison.domain.model.backup.BackupContent
import takagi.ru.saison.domain.model.backup.BackupFile
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.WebDavConfig
import takagi.ru.saison.util.backup.BackupFileManager
import takagi.ru.saison.util.backup.DataExporter
import takagi.ru.saison.util.backup.DataImporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebDAV 备份仓库实现
 * 
 * 重构说明:
 * - 使用 WebDavConfigStorage 统一管理所有配置存储
 * - 使用 WebDavPathManager 统一管理路径生成
 * - 简化代码逻辑，提高可维护性
 * - 保持向后兼容性（通过 WebDavConfigStorage 的自动迁移）
 */
@Singleton
class WebDavBackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webDavClient: WebDavClient,
    private val backupFileManager: BackupFileManager,
    private val dataExporter: DataExporter,
    private val dataImporter: DataImporter,
    private val configStorage: WebDavConfigStorage,
    private val pathManager: WebDavPathManager
) : WebDavBackupRepository {
    
    companion object {
        private const val TAG = "WebDavBackupRepo"
    }
    
    // ==================== 服务器配置管理 ====================
    
    override suspend fun configure(url: String, username: String, password: String) {
        try {
            val normalizedUrl = pathManager.normalizeServerUrl(url)
            configStorage.saveServerConfig(normalizedUrl, username, password)
            android.util.Log.d(TAG, "配置已保存: $normalizedUrl, $username")
        } catch (e: IllegalArgumentException) {
            android.util.Log.e(TAG, "无效的 URL", e)
            throw e
        }
    }
    
    override suspend fun isConfigured(): Boolean {
        return configStorage.isServerConfigured()
    }
    
    override suspend fun getCurrentConfig(): WebDavConfig? {
        return configStorage.getServerConfig()
    }
    
    override suspend fun clearConfig() {
        configStorage.clearAll()
        android.util.Log.d(TAG, "配置已清除")
    }
    
    /**
     * 获取凭证（内部使用）
     */
    private fun getCredentials(): WebDavCredentials {
        val config = configStorage.getServerConfig()
            ?: throw IllegalStateException("WebDAV 未配置")
        return WebDavCredentials(config.username, configStorage.getPassword())
    }
    
    // ==================== 连接测试 ====================
    
    override suspend fun testConnection(): Result<Boolean> {
        return try {
            android.util.Log.d(TAG, "开始测试连接")
            
            val config = getCurrentConfig() ?: return Result.failure(Exception("未配置 WebDAV"))
            val credentials = getCredentials()
            val backupDir = pathManager.getBackupDirPath(config.serverUrl)
            
            android.util.Log.d(TAG, "测试基本连接: ${config.serverUrl}")
            val connected = webDavClient.checkConnection(config.serverUrl, credentials)
            if (!connected) {
                android.util.Log.e(TAG, "基本连接失败")
                return Result.failure(Exception("连接失败，请检查服务器地址和网络"))
            }
            
            android.util.Log.d(TAG, "基本连接成功，创建备份目录: $backupDir")
            val dirCreated = webDavClient.createDirectory(backupDir, credentials)
            if (!dirCreated) {
                android.util.Log.e(TAG, "创建备份目录失败")
                return Result.failure(Exception("无法创建备份目录"))
            }
            
            android.util.Log.d(TAG, "测试写权限")
            val writeTest = webDavClient.testWritePermission(backupDir, credentials)
            writeTest.fold(
                onSuccess = {
                    android.util.Log.d(TAG, "连接测试成功")
                    Result.success(true)
                },
                onFailure = { error ->
                    android.util.Log.e(TAG, "写权限测试失败", error)
                    Result.failure(Exception("没有写入权限: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "连接测试异常", e)
            Result.failure(e)
        }
    }
    
    // ==================== 自动备份管理 ====================
    
    override suspend fun configureAutoBackup(enabled: Boolean) {
        configStorage.setAutoBackupEnabled(enabled)
        android.util.Log.d(TAG, "自动备份已${if (enabled) "启用" else "禁用"}")
    }
    
    override suspend fun isAutoBackupEnabled(): Boolean {
        return configStorage.isAutoBackupEnabled()
    }
    
    override suspend fun shouldAutoBackup(): Boolean {
        return configStorage.shouldAutoBackup()
    }
    
    override suspend fun updateLastBackupTime() {
        configStorage.updateLastBackupTime()
        android.util.Log.d(TAG, "最后备份时间已更新")
    }
    
    override suspend fun getLastBackupTime(): Long {
        return configStorage.getLastBackupTime()
    }
    
    // ==================== 备份偏好设置管理 ====================
    
    override suspend fun saveBackupPreferences(preferences: BackupPreferences) {
        configStorage.saveBackupPreferences(preferences)
        android.util.Log.d(TAG, "备份偏好已保存: $preferences")
    }
    
    override suspend fun getBackupPreferences(): BackupPreferences {
        return configStorage.getBackupPreferences()
    }
    
    // ==================== 备份创建和上传 ====================
    
    override suspend fun createAndUploadBackup(preferences: BackupPreferences, dataFiles: Map<String, String>): Result<String> {
        return try {
            android.util.Log.d(TAG, "开始创建并上传备份")
            
            if (!preferences.hasAnyEnabled()) {
                android.util.Log.e(TAG, "没有启用任何备份类型")
                return Result.failure(Exception("请至少选择一种备份内容"))
            }
            
            val config = getCurrentConfig()
                ?: return Result.failure(Exception("未配置 WebDAV"))
            
            android.util.Log.d(TAG, "配置: ${config.serverUrl}, ${config.username}")
            
            val credentials = getCredentials()
            
            // 生成备份文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "saison_backup_$timestamp.zip"
            android.util.Log.d(TAG, "备份文件名: $fileName")
            
            // 创建 ZIP 文件
            val zipFile = File(context.cacheDir, fileName)
            android.util.Log.d(TAG, "创建 ZIP 文件: ${zipFile.absolutePath}")
            android.util.Log.d(TAG, "数据文件数量: ${dataFiles.size}")
            
            backupFileManager.createZipArchive(dataFiles, zipFile)
            android.util.Log.d(TAG, "ZIP 文件创建完成，大小: ${zipFile.length()} bytes")
            
            // 确保备份目录存在
            val backupDir = pathManager.getBackupDirPath(config.serverUrl)
            android.util.Log.d(TAG, "确保备份目录存在: $backupDir")
            val dirCreated = webDavClient.createDirectory(backupDir, credentials)
            if (!dirCreated) {
                android.util.Log.w(TAG, "创建目录失败或目录已存在")
            }
            
            // 上传到 WebDAV
            val uploadUrl = pathManager.getBackupFilePath(config.serverUrl, fileName)
            android.util.Log.d(TAG, "上传 URL: $uploadUrl")
            
            val uploadResult = webDavClient.uploadFile(uploadUrl, credentials, zipFile)
            
            // 清理临时文件
            zipFile.delete()
            
            uploadResult.fold(
                onSuccess = {
                    updateLastBackupTime()
                    android.util.Log.d(TAG, "备份成功: $fileName")
                    Result.success(fileName)
                },
                onFailure = { error ->
                    android.util.Log.e(TAG, "上传失败", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "备份异常", e)
            Result.failure(e)
        }
    }
    
    // ==================== 备份列表查询 ====================
    
    override suspend fun listBackups(): Result<List<BackupFile>> {
        return try {
            val config = getCurrentConfig() 
                ?: return Result.failure(Exception("未配置 WebDAV"))
            
            val credentials = getCredentials()
            val backupDir = pathManager.getBackupDirPath(config.serverUrl)
            
            android.util.Log.d(TAG, "列出备份文件: $backupDir")
            val files = webDavClient.listFiles(backupDir, credentials)
            
            val backupFiles = files.map { fileInfo ->
                BackupFile(
                    name = fileInfo.name,
                    size = fileInfo.size,
                    modified = Date(fileInfo.modified),
                    url = fileInfo.url
                )
            }.sortedByDescending { it.modified }
            
            android.util.Log.d(TAG, "找到 ${backupFiles.size} 个备份文件")
            Result.success(backupFiles)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "列出备份失败", e)
            Result.failure(e)
        }
    }
    
    // ==================== 备份下载和恢复 ====================
    
    override suspend fun downloadAndRestoreBackup(backupFile: BackupFile): Result<BackupContent> {
        return try {
            android.util.Log.d(TAG, "开始下载并恢复备份: ${backupFile.name}")
            
            val config = getCurrentConfig() 
                ?: return Result.failure(Exception("未配置 WebDAV"))
            
            val credentials = getCredentials()
            
            // 下载备份文件
            val downloadedFile = File(context.cacheDir, "restore_${backupFile.name}")
            android.util.Log.d(TAG, "下载到: ${downloadedFile.absolutePath}")
            
            val downloaded = webDavClient.downloadFile(backupFile.url, credentials, downloadedFile)
            
            if (!downloaded) {
                android.util.Log.e(TAG, "下载失败")
                return Result.failure(Exception("下载失败"))
            }
            
            android.util.Log.d(TAG, "下载成功，开始解压")
            // 解压备份文件
            val extractedFiles = backupFileManager.extractZipArchive(downloadedFile)
            
            android.util.Log.d(TAG, "解压成功，开始导入数据")
            // 导入数据
            val tasks = extractedFiles["tasks.json"]?.let { dataImporter.importTasks(it) } ?: emptyList()
            val courses = extractedFiles["courses.json"]?.let { dataImporter.importCourses(it) } ?: emptyList()
            val events = extractedFiles["events.json"]?.let { dataImporter.importEvents(it) } ?: emptyList()
            val routines = extractedFiles["routines.json"]?.let { dataImporter.importRoutines(it) } ?: emptyList()
            val subscriptions = extractedFiles["subscriptions.json"]?.let { dataImporter.importSubscriptions(it) } ?: emptyList()
            val pomodoroSessions = extractedFiles["pomodoro_sessions.json"]?.let { dataImporter.importPomodoroSessions(it) } ?: emptyList()
            val semesters = extractedFiles["semesters.json"]?.let { dataImporter.importSemesters(it) } ?: emptyList()
            val categories = extractedFiles["categories.json"]?.let { dataImporter.importCategories(it) } ?: emptyList()
            val preferences = extractedFiles["preferences.json"]?.let { dataImporter.importPreferences(it) } ?: emptyMap()
            
            // 清理临时文件
            downloadedFile.delete()
            
            val content = BackupContent(
                tasks = tasks,
                courses = courses,
                events = events,
                routines = routines,
                subscriptions = subscriptions,
                pomodoroSessions = pomodoroSessions,
                semesters = semesters,
                categories = categories,
                preferences = preferences
            )
            
            android.util.Log.d(TAG, "恢复成功: ${tasks.size} 任务, ${courses.size} 课程")
            Result.success(content)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "恢复失败", e)
            Result.failure(e)
        }
    }
    
    // ==================== 备份删除 ====================
    
    override suspend fun deleteBackup(backupFile: BackupFile): Result<Unit> {
        return try {
            android.util.Log.d(TAG, "删除备份: ${backupFile.name}")
            
            val config = getCurrentConfig() 
                ?: return Result.failure(Exception("未配置 WebDAV"))
            
            val credentials = getCredentials()
            
            val deleted = webDavClient.deleteFile(backupFile.url, credentials)
            if (deleted) {
                android.util.Log.d(TAG, "删除成功")
                Result.success(Unit)
            } else {
                android.util.Log.e(TAG, "删除失败")
                Result.failure(Exception("删除失败"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "删除异常", e)
            Result.failure(e)
        }
    }
}
