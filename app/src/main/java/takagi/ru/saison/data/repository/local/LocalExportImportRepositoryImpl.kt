package takagi.ru.saison.data.repository.local

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.PomodoroRepository
import takagi.ru.saison.data.repository.RoutineRepositoryImpl
import takagi.ru.saison.data.repository.SemesterRepositoryImpl
import takagi.ru.saison.data.repository.SubscriptionRepository
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.domain.mapper.toDomain
import takagi.ru.saison.domain.mapper.toEntity
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.backup.ExportSummary
import takagi.ru.saison.domain.model.backup.ImportPreview
import takagi.ru.saison.domain.model.backup.RestoreSummary
import takagi.ru.saison.util.backup.BackupFileManager
import takagi.ru.saison.util.backup.DataExporter
import takagi.ru.saison.util.backup.DataImporter
import takagi.ru.saison.util.backup.DataTypeDetector
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.exportPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "export_preferences"
)

@Singleton
class LocalExportImportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository,
    private val routineRepository: RoutineRepositoryImpl,
    private val subscriptionRepository: SubscriptionRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val semesterRepository: SemesterRepositoryImpl,
    private val dataExporter: DataExporter,
    private val dataImporter: DataImporter,
    private val backupFileManager: BackupFileManager,
    private val dataTypeDetector: DataTypeDetector,
    private val duplicateDetector: takagi.ru.saison.util.backup.DuplicateDetector
) : LocalExportImportRepository {
    
    private val dataStore = context.exportPrefsDataStore
    
    /**
     * 创建临时目录用于导出导入操作
     * 确保操作完成后清理临时文件
     */
    private fun createTempDirectory(): File {
        val tempDir = File(context.cacheDir, "export_import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        return tempDir
    }
    
    /**
     * 清理临时目录及其所有内容
     */
    private fun cleanupTempDirectory(tempDir: File?) {
        try {
            tempDir?.deleteRecursively()
        } catch (e: Exception) {
            // 记录错误但不抛出异常，因为这是清理操作
            android.util.Log.e("LocalExportImport", "Failed to cleanup temp directory", e)
        }
    }
    
    /**
     * 使用临时目录执行操作，确保操作完成后清理
     */
    private inline fun <T> withTempDirectory(block: (File) -> T): T {
        val tempDir = createTempDirectory()
        try {
            return block(tempDir)
        } finally {
            cleanupTempDirectory(tempDir)
        }
    }
    
    private object PrefsKeys {
        val INCLUDE_TASKS = booleanPreferencesKey("include_tasks")
        val INCLUDE_COURSES = booleanPreferencesKey("include_courses")
        val INCLUDE_EVENTS = booleanPreferencesKey("include_events")
        val INCLUDE_ROUTINES = booleanPreferencesKey("include_routines")
        val INCLUDE_SUBSCRIPTIONS = booleanPreferencesKey("include_subscriptions")
        val INCLUDE_POMODORO = booleanPreferencesKey("include_pomodoro")
        val INCLUDE_SEMESTERS = booleanPreferencesKey("include_semesters")
        val INCLUDE_PREFERENCES = booleanPreferencesKey("include_preferences")
    }
    
    override suspend fun exportToZip(
        uri: Uri,
        preferences: BackupPreferences
    ): Result<ExportSummary> {
        return try {
            // 验证至少有一个数据类型被启用
            if (!preferences.hasAnyEnabled()) {
                return Result.failure(IllegalArgumentException("至少需要选择一个数据类型"))
            }
            
            // 收集要导出的数据
            val jsonFiles = mutableMapOf<String, String>()
            var totalItems = 0
            val exportedTypes = mutableListOf<DataType>()
            
            if (preferences.includeTasks) {
                val tasks = taskRepository.getAllTasks().first()
                jsonFiles[DataType.TASKS.fileName] = dataExporter.exportTasks(tasks)
                totalItems += tasks.size
                exportedTypes.add(DataType.TASKS)
            }
            
            if (preferences.includeCourses) {
                val courses = courseRepository.getAllCourses().first()
                jsonFiles[DataType.COURSES.fileName] = dataExporter.exportCourses(courses)
                totalItems += courses.size
                exportedTypes.add(DataType.COURSES)
            }
            
            // Events 功能暂未实现，跳过
            // if (preferences.includeEvents) { ... }
            
            if (preferences.includeRoutines) {
                val routines = routineRepository.getAllRoutineTasks().first()
                jsonFiles[DataType.ROUTINES.fileName] = dataExporter.exportRoutines(routines)
                totalItems += routines.size
                exportedTypes.add(DataType.ROUTINES)
            }
            
            if (preferences.includeSubscriptions) {
                val subscriptionEntities = subscriptionRepository.getAllSubscriptions().first()
                val subscriptions = subscriptionEntities.map { it.toDomain() }
                jsonFiles[DataType.SUBSCRIPTIONS.fileName] = dataExporter.exportSubscriptions(subscriptions)
                totalItems += subscriptions.size
                exportedTypes.add(DataType.SUBSCRIPTIONS)
            }
            
            if (preferences.includePomodoroSessions) {
                val sessions = pomodoroRepository.getAllSessions().first()
                jsonFiles[DataType.POMODORO_SESSIONS.fileName] = dataExporter.exportPomodoroSessions(sessions)
                totalItems += sessions.size
                exportedTypes.add(DataType.POMODORO_SESSIONS)
            }
            
            if (preferences.includeSemesters) {
                val semesters = semesterRepository.getAllSemesters().first()
                jsonFiles[DataType.SEMESTERS.fileName] = dataExporter.exportSemesters(semesters)
                totalItems += semesters.size
                exportedTypes.add(DataType.SEMESTERS)
            }
            
            // 创建临时 ZIP 文件
            val tempDir = context.cacheDir.resolve("export_temp")
            tempDir.mkdirs()
            val tempZipFile = tempDir.resolve("temp_backup.zip")
            
            try {
                // 创建 ZIP 文件
                backupFileManager.createZipArchive(jsonFiles, tempZipFile)
                
                // 写入到用户选择的位置
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    tempZipFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val fileSize = tempZipFile.length()
                
                Result.success(
                    ExportSummary(
                        totalItems = totalItems,
                        exportedTypes = exportedTypes,
                        filePath = uri.toString(),
                        fileSize = fileSize
                    )
                )
            } finally {
                // 清理临时文件
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun exportToJson(
        uri: Uri,
        dataType: DataType
    ): Result<ExportSummary> {
        return try {
            val jsonContent = when (dataType) {
                DataType.TASKS -> {
                    val tasks = taskRepository.getAllTasks().first()
                    dataExporter.exportTasks(tasks) to tasks.size
                }
                DataType.COURSES -> {
                    val courses = courseRepository.getAllCourses().first()
                    dataExporter.exportCourses(courses) to courses.size
                }
                DataType.ROUTINES -> {
                    val routines = routineRepository.getAllRoutineTasks().first()
                    dataExporter.exportRoutines(routines) to routines.size
                }
                DataType.SUBSCRIPTIONS -> {
                    val subscriptionEntities = subscriptionRepository.getAllSubscriptions().first()
                    val subscriptions = subscriptionEntities.map { it.toDomain() }
                    dataExporter.exportSubscriptions(subscriptions) to subscriptions.size
                }
                DataType.POMODORO_SESSIONS -> {
                    val sessions = pomodoroRepository.getAllSessions().first()
                    dataExporter.exportPomodoroSessions(sessions) to sessions.size
                }
                DataType.SEMESTERS -> {
                    val semesters = semesterRepository.getAllSemesters().first()
                    dataExporter.exportSemesters(semesters) to semesters.size
                }
                else -> "[]" to 0
            }
            
            // 写入到用户选择的位置
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonContent.first.toByteArray(Charsets.UTF_8))
            }
            
            val fileSize = jsonContent.first.toByteArray(Charsets.UTF_8).size.toLong()
            
            Result.success(
                ExportSummary(
                    totalItems = jsonContent.second,
                    exportedTypes = listOf(dataType),
                    filePath = uri.toString(),
                    fileSize = fileSize
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun importFromZip(uri: Uri): Result<RestoreSummary> {
        val tempDir = context.cacheDir.resolve("import_temp")
        
        return try {
            tempDir.mkdirs()
            val tempZipFile = tempDir.resolve("temp_import.zip")
            
            // 复制 ZIP 文件到临时位置
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempZipFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // 解压 ZIP 文件
            val extractedFiles = backupFileManager.extractZipArchive(tempZipFile)
            
            // 导入每个文件
            var importedTasks = 0
            var importedCourses = 0
            var importedRoutines = 0
            var importedSubscriptions = 0
            var importedPomodoro = 0
            var importedSemesters = 0
            var skippedDuplicates = 0
            
            // 导入任务
            extractedFiles[DataType.TASKS.fileName]?.let { json ->
                val tasks = dataImporter.importTasks(json)
                val existingTasks = taskRepository.getAllTasks().first()
                
                tasks.forEach { task ->
                    if (!duplicateDetector.isTaskDuplicate(task, existingTasks)) {
                        taskRepository.insertTask(task)
                        importedTasks++
                    } else {
                        skippedDuplicates++
                    }
                }
            }
            
            // 导入课程
            extractedFiles[DataType.COURSES.fileName]?.let { json ->
                val courses = dataImporter.importCourses(json)
                val existingCourses = courseRepository.getAllCourses().first()
                
                courses.forEach { course ->
                    if (!duplicateDetector.isCourseDuplicate(course, existingCourses)) {
                        courseRepository.insertCourse(course)
                        importedCourses++
                    } else {
                        skippedDuplicates++
                    }
                }
            }
            
            // 导入例行任务
            extractedFiles[DataType.ROUTINES.fileName]?.let { json ->
                val routines = dataImporter.importRoutines(json)
                val existingRoutines = routineRepository.getAllRoutineTasks().first()
                
                routines.forEach { routine ->
                    if (!duplicateDetector.isRoutineDuplicate(routine, existingRoutines)) {
                        routineRepository.createRoutineTask(routine)
                        importedRoutines++
                    } else {
                        skippedDuplicates++
                    }
                }
            }
            
            // 导入订阅
            extractedFiles[DataType.SUBSCRIPTIONS.fileName]?.let { json ->
                val subscriptions = dataImporter.importSubscriptions(json)
                val existingSubscriptionEntities = subscriptionRepository.getAllSubscriptions().first()
                val existingSubscriptions = existingSubscriptionEntities.map { it.toDomain() }
                
                subscriptions.forEach { subscription ->
                    if (!duplicateDetector.isSubscriptionDuplicate(subscription, existingSubscriptions)) {
                        subscriptionRepository.insertSubscription(subscription.toEntity())
                        importedSubscriptions++
                    } else {
                        skippedDuplicates++
                    }
                }
            }
            
            // 导入番茄钟记录
            extractedFiles[DataType.POMODORO_SESSIONS.fileName]?.let { json ->
                val sessions = dataImporter.importPomodoroSessions(json)
                val existingSessions = pomodoroRepository.getAllSessions().first()
                
                sessions.forEach { session ->
                    if (!duplicateDetector.isPomodoroDuplicate(session, existingSessions)) {
                        pomodoroRepository.insertSession(session)
                        importedPomodoro++
                    } else {
                        skippedDuplicates++
                    }
                }
            }
            
            // 导入学期
            extractedFiles[DataType.SEMESTERS.fileName]?.let { json ->
                val semesters = dataImporter.importSemesters(json)
                val existingSemesters = semesterRepository.getAllSemesters().first()
                
                semesters.forEach { semester ->
                    if (!duplicateDetector.isSemesterDuplicate(semester, existingSemesters)) {
                        semesterRepository.insertSemester(semester)
                        importedSemesters++
                    } else {
                        skippedDuplicates++
                    }
                }
            }
            
            Result.success(
                RestoreSummary(
                    importedTasks = importedTasks,
                    importedCourses = importedCourses,
                    importedRoutines = importedRoutines,
                    importedSubscriptions = importedSubscriptions,
                    importedPomodoroSessions = importedPomodoro,
                    importedSemesters = importedSemesters,
                    skippedDuplicates = skippedDuplicates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // 清理临时文件
            tempDir.deleteRecursively()
        }
    }
    
    override suspend fun importFromJson(
        uri: Uri,
        dataType: DataType?
    ): Result<RestoreSummary> {
        return try {
            // 读取 JSON 内容
            val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: return Result.failure(IllegalArgumentException("无法读取文件"))
            
            // 如果未指定数据类型，尝试自动检测
            val detectedType = dataType ?: dataTypeDetector.detectDataType(jsonContent)
            ?: return Result.failure(IllegalArgumentException("无法检测数据类型"))
            
            var importedCount = 0
            var skippedDuplicates = 0
            
            when (detectedType) {
                DataType.TASKS -> {
                    val tasks = dataImporter.importTasks(jsonContent)
                    val existingTasks = taskRepository.getAllTasks().first()
                    
                    tasks.forEach { task ->
                        if (!duplicateDetector.isTaskDuplicate(task, existingTasks)) {
                            taskRepository.insertTask(task)
                            importedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                    
                    return Result.success(RestoreSummary(importedTasks = importedCount, skippedDuplicates = skippedDuplicates))
                }
                
                DataType.COURSES -> {
                    val courses = dataImporter.importCourses(jsonContent)
                    val existingCourses = courseRepository.getAllCourses().first()
                    
                    courses.forEach { course ->
                        if (!duplicateDetector.isCourseDuplicate(course, existingCourses)) {
                            courseRepository.insertCourse(course)
                            importedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                    
                    return Result.success(RestoreSummary(importedCourses = importedCount, skippedDuplicates = skippedDuplicates))
                }
                
                DataType.ROUTINES -> {
                    val routines = dataImporter.importRoutines(jsonContent)
                    val existingRoutines = routineRepository.getAllRoutineTasks().first()
                    
                    routines.forEach { routine ->
                        if (!duplicateDetector.isRoutineDuplicate(routine, existingRoutines)) {
                            routineRepository.createRoutineTask(routine)
                            importedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                    
                    return Result.success(RestoreSummary(importedRoutines = importedCount, skippedDuplicates = skippedDuplicates))
                }
                
                DataType.SUBSCRIPTIONS -> {
                    val subscriptions = dataImporter.importSubscriptions(jsonContent)
                    val existingSubscriptionEntities = subscriptionRepository.getAllSubscriptions().first()
                    val existingSubscriptions = existingSubscriptionEntities.map { it.toDomain() }
                    
                    subscriptions.forEach { subscription ->
                        if (!duplicateDetector.isSubscriptionDuplicate(subscription, existingSubscriptions)) {
                            subscriptionRepository.insertSubscription(subscription.toEntity())
                            importedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                    
                    return Result.success(RestoreSummary(importedSubscriptions = importedCount, skippedDuplicates = skippedDuplicates))
                }
                
                DataType.POMODORO_SESSIONS -> {
                    val sessions = dataImporter.importPomodoroSessions(jsonContent)
                    val existingSessions = pomodoroRepository.getAllSessions().first()
                    
                    sessions.forEach { session ->
                        if (!duplicateDetector.isPomodoroDuplicate(session, existingSessions)) {
                            pomodoroRepository.insertSession(session)
                            importedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                    
                    return Result.success(RestoreSummary(importedPomodoroSessions = importedCount, skippedDuplicates = skippedDuplicates))
                }
                
                DataType.SEMESTERS -> {
                    val semesters = dataImporter.importSemesters(jsonContent)
                    val existingSemesters = semesterRepository.getAllSemesters().first()
                    
                    semesters.forEach { semester ->
                        if (!duplicateDetector.isSemesterDuplicate(semester, existingSemesters)) {
                            semesterRepository.insertSemester(semester)
                            importedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                    
                    return Result.success(RestoreSummary(importedSemesters = importedCount, skippedDuplicates = skippedDuplicates))
                }
                
                else -> Result.failure(IllegalArgumentException("不支持的数据类型"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun previewImport(uri: Uri): Result<ImportPreview> {
        val tempDir = context.cacheDir.resolve("preview_temp")
        
        return try {
            tempDir.mkdirs()
            
            // 检查文件类型
            val mimeType = context.contentResolver.getType(uri)
            val isZipFile = mimeType == "application/zip" || uri.toString().endsWith(".zip")
            
            val dataTypeCounts = mutableMapOf<DataType, Int>()
            var totalNew = 0
            var totalDuplicates = 0
            
            if (isZipFile) {
                // 处理 ZIP 文件
                val tempZipFile = tempDir.resolve("temp_preview.zip")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempZipFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val extractedFiles = backupFileManager.extractZipArchive(tempZipFile)
                
                // 预览每种数据类型
                extractedFiles[DataType.TASKS.fileName]?.let { json ->
                    val tasks = dataImporter.importTasks(json)
                    val existingTasks = taskRepository.getAllTasks().first()
                    val (newCount, dupCount) = countNewAndDuplicates(
                        tasks, existingTasks
                    ) { task, existing -> duplicateDetector.isTaskDuplicate(task, existing) }
                    
                    dataTypeCounts[DataType.TASKS] = tasks.size
                    totalNew += newCount
                    totalDuplicates += dupCount
                }
                
                extractedFiles[DataType.COURSES.fileName]?.let { json ->
                    val courses = dataImporter.importCourses(json)
                    val existingCourses = courseRepository.getAllCourses().first()
                    val (newCount, dupCount) = countNewAndDuplicates(
                        courses, existingCourses
                    ) { course, existing -> duplicateDetector.isCourseDuplicate(course, existing) }
                    
                    dataTypeCounts[DataType.COURSES] = courses.size
                    totalNew += newCount
                    totalDuplicates += dupCount
                }
                
                // 其他数据类型类似处理...
                
            } else {
                // 处理单个 JSON 文件
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes().toString(Charsets.UTF_8)
                } ?: return Result.failure(IllegalArgumentException("无法读取文件"))
                
                val detectedType = dataTypeDetector.detectDataType(jsonContent)
                ?: return Result.failure(IllegalArgumentException("无法检测数据类型"))
                
                when (detectedType) {
                    DataType.TASKS -> {
                        val tasks = dataImporter.importTasks(jsonContent)
                        val existingTasks = taskRepository.getAllTasks().first()
                        val (newCount, dupCount) = countNewAndDuplicates(
                            tasks, existingTasks
                        ) { task, existing -> duplicateDetector.isTaskDuplicate(task, existing) }
                        
                        dataTypeCounts[DataType.TASKS] = tasks.size
                        totalNew = newCount
                        totalDuplicates = dupCount
                    }
                    // 其他数据类型类似处理...
                    else -> {}
                }
            }
            
            Result.success(
                ImportPreview(
                    dataTypes = dataTypeCounts,
                    totalItems = dataTypeCounts.values.sum(),
                    newItems = totalNew,
                    duplicateItems = totalDuplicates,
                    isZipFile = isZipFile
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    /**
     * 计算新项目和重复项目的数量
     */
    private fun <T> countNewAndDuplicates(
        items: List<T>,
        existing: List<T>,
        isDuplicate: (T, List<T>) -> Boolean
    ): Pair<Int, Int> {
        var newCount = 0
        var dupCount = 0
        
        items.forEach { item ->
            if (isDuplicate(item, existing)) {
                dupCount++
            } else {
                newCount++
            }
        }
        
        return Pair(newCount, dupCount)
    }
    
    override suspend fun saveExportPreferences(preferences: BackupPreferences) {
        dataStore.edit { prefs ->
            prefs[PrefsKeys.INCLUDE_TASKS] = preferences.includeTasks
            prefs[PrefsKeys.INCLUDE_COURSES] = preferences.includeCourses
            prefs[PrefsKeys.INCLUDE_EVENTS] = preferences.includeEvents
            prefs[PrefsKeys.INCLUDE_ROUTINES] = preferences.includeRoutines
            prefs[PrefsKeys.INCLUDE_SUBSCRIPTIONS] = preferences.includeSubscriptions
            prefs[PrefsKeys.INCLUDE_POMODORO] = preferences.includePomodoroSessions
            prefs[PrefsKeys.INCLUDE_SEMESTERS] = preferences.includeSemesters
            prefs[PrefsKeys.INCLUDE_PREFERENCES] = preferences.includePreferences
        }
    }
    
    override suspend fun getExportPreferences(): BackupPreferences {
        return dataStore.data.map { prefs ->
            BackupPreferences(
                includeTasks = prefs[PrefsKeys.INCLUDE_TASKS] ?: true,
                includeCourses = prefs[PrefsKeys.INCLUDE_COURSES] ?: true,
                includeEvents = prefs[PrefsKeys.INCLUDE_EVENTS] ?: true,
                includeRoutines = prefs[PrefsKeys.INCLUDE_ROUTINES] ?: true,
                includeSubscriptions = prefs[PrefsKeys.INCLUDE_SUBSCRIPTIONS] ?: true,
                includePomodoroSessions = prefs[PrefsKeys.INCLUDE_POMODORO] ?: true,
                includeSemesters = prefs[PrefsKeys.INCLUDE_SEMESTERS] ?: true,
                includePreferences = prefs[PrefsKeys.INCLUDE_PREFERENCES] ?: true
            )
        }.first()
    }
    
    override suspend fun getDataCounts(): Map<DataType, Int> {
        return mapOf(
            DataType.TASKS to taskRepository.getAllTasks().first().size,
            DataType.COURSES to courseRepository.getAllCourses().first().size,
            DataType.ROUTINES to routineRepository.getAllRoutineTasks().first().size,
            DataType.SUBSCRIPTIONS to subscriptionRepository.getAllSubscriptions().first().size,
            DataType.POMODORO_SESSIONS to pomodoroRepository.getAllSessions().first().size,
            DataType.SEMESTERS to semesterRepository.getAllSemesters().first().size
        )
    }
    
    /**
     * 生成带时间戳的文件名
     */
    fun generateFileName(prefix: String, extension: String): String {
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        )
        return "${prefix}_${timestamp}.${extension}"
    }
}
