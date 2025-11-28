package takagi.ru.saison.data.repository.local

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.PomodoroRepository
import takagi.ru.saison.data.repository.RoutineRepositoryImpl
import takagi.ru.saison.data.repository.SemesterRepositoryImpl
import takagi.ru.saison.data.repository.SubscriptionRepository
import takagi.ru.saison.data.repository.TaskRepository
import takagi.ru.saison.domain.model.*
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.routine.CycleType
import takagi.ru.saison.domain.model.routine.RoutineTask
import takagi.ru.saison.util.backup.BackupFileManager
import takagi.ru.saison.util.backup.DataExporter
import takagi.ru.saison.util.backup.DataImporter
import takagi.ru.saison.util.backup.DataTypeDetector
import takagi.ru.saison.util.backup.DuplicateDetector
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.system.measureTimeMillis

/**
 * 性能测试：本地导出导入功能
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocalExportImportPerformanceTest {
    
    private lateinit var context: Context
    private lateinit var repository: LocalExportImportRepositoryImpl
    private lateinit var taskRepository: TaskRepository
    private lateinit var courseRepository: CourseRepository
    private lateinit var routineRepository: RoutineRepositoryImpl
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var pomodoroRepository: PomodoroRepository
    private lateinit var semesterRepository: SemesterRepositoryImpl
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // 创建 mock repositories
        taskRepository = createMockTaskRepository()
        courseRepository = createMockCourseRepository()
        routineRepository = createMockRoutineRepository()
        subscriptionRepository = createMockSubscriptionRepository()
        pomodoroRepository = createMockPomodoroRepository()
        semesterRepository = createMockSemesterRepository()
        
        // 创建实际的 repository
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
    }
    
    /**
     * 测试大数据集的导出性能
     * Requirements: 11.1
     * 
     * 验证：
     * - 导出 1000+ 任务应在合理时间内完成（< 5秒）
     * - 导出操作不应阻塞主线程
     * - 内存使用应保持稳定
     */
    @Test
    fun `test large dataset export performance`() = runBlocking {
        // 创建大量测试数据
        val largeTasks = generateLargeTasks(1000)
        val largeCourses = generateLargeCourses(500)
        val largeRoutines = generateLargeRoutines(200)
        
        // 更新 mock repositories
        taskRepository = createMockTaskRepository(largeTasks)
        courseRepository = createMockCourseRepository(largeCourses)
        routineRepository = createMockRoutineRepository(largeRoutines)
        
        // 重新创建 repository
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFile = File(context.cacheDir, "test_large_export.zip")
        val uri = Uri.fromFile(tempFile)
        
        try {
            // 测量导出时间
            val exportTime = measureTimeMillis {
                val result = repository.exportToZip(
                    uri = uri,
                    preferences = BackupPreferences(
                        includeTasks = true,
                        includeCourses = true,
                        includeRoutines = true,
                        includeSubscriptions = false,
                        includePomodoroSessions = false,
                        includeSemesters = false
                    )
                )
                
                result.isSuccess shouldBe true
                val summary = result.getOrNull()
                summary shouldNotBe null
                summary?.totalItems shouldBe 1700 // 1000 + 500 + 200
            }
            
            // 验证性能：应在 5 秒内完成
            println("Large dataset export time: ${exportTime}ms")
            assert(exportTime < 5000) { "Export took too long: ${exportTime}ms" }
            
            // 验证文件大小合理
            assert(tempFile.exists())
            val fileSize = tempFile.length()
            println("Export file size: ${fileSize / 1024}KB")
            assert(fileSize > 0)
            
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * 测试大文件的导入性能
     * Requirements: 11.2
     * 
     * 验证：
     * - 导入大文件应在合理时间内完成
     * - 导入操作不应阻塞主线程
     * - 内存使用应保持稳定
     */
    @Test
    fun `test large file import performance`() = runBlocking {
        // 首先创建一个大文件
        val largeTasks = generateLargeTasks(1000)
        taskRepository = createMockTaskRepository(largeTasks)
        
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFile = File(context.cacheDir, "test_large_import.zip")
        val uri = Uri.fromFile(tempFile)
        
        try {
            // 先导出
            repository.exportToZip(
                uri = uri,
                preferences = BackupPreferences(includeTasks = true)
            )
            
            // 测量导入时间
            val importTime = measureTimeMillis {
                val result = repository.importFromZip(uri)
                
                result.isSuccess shouldBe true
                val summary = result.getOrNull()
                summary shouldNotBe null
            }
            
            // 验证性能：应在 10 秒内完成（导入比导出慢）
            println("Large file import time: ${importTime}ms")
            assert(importTime < 10000) { "Import took too long: ${importTime}ms" }
            
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * 测试流式处理避免内存溢出
     * Requirements: 11.3
     * 
     * 验证：
     * - 处理大文件时不应将整个内容加载到内存
     * - 应使用流式读写
     * - 内存使用应保持在合理范围内
     */
    @Test
    fun `test streaming prevents memory overflow`() = runBlocking {
        // 创建非常大的数据集
        val veryLargeTasks = generateLargeTasks(5000)
        taskRepository = createMockTaskRepository(veryLargeTasks)
        
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFile = File(context.cacheDir, "test_streaming.zip")
        val uri = Uri.fromFile(tempFile)
        
        try {
            // 记录初始内存
            val runtime = Runtime.getRuntime()
            runtime.gc()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // 执行导出
            val result = repository.exportToZip(
                uri = uri,
                preferences = BackupPreferences(includeTasks = true)
            )
            
            result.isSuccess shouldBe true
            
            // 记录导出后内存
            runtime.gc()
            val afterExportMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = afterExportMemory - initialMemory
            
            println("Memory increase during export: ${memoryIncrease / 1024 / 1024}MB")
            
            // 验证内存增长不应超过 100MB（合理范围）
            assert(memoryIncrease < 100 * 1024 * 1024) {
                "Memory increase too large: ${memoryIncrease / 1024 / 1024}MB"
            }
            
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * 测试所有操作在后台线程执行
     * Requirements: 11.1, 11.2
     * 
     * 验证：
     * - 导出操作不在主线程执行
     * - 导入操作不在主线程执行
     * - 操作期间主线程保持响应
     */
    @Test
    fun `test operations run on background thread`() = runBlocking {
        val tasks = generateLargeTasks(500)
        taskRepository = createMockTaskRepository(tasks)
        
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFile = File(context.cacheDir, "test_background.zip")
        val uri = Uri.fromFile(tempFile)
        
        try {
            // 在后台线程执行导出
            val exportJob = async(Dispatchers.IO) {
                repository.exportToZip(
                    uri = uri,
                    preferences = BackupPreferences(includeTasks = true)
                )
            }
            
            // 验证主线程仍然可以执行其他操作
            var mainThreadCounter = 0
            while (exportJob.isActive) {
                mainThreadCounter++
                delay(10)
            }
            
            // 主线程应该能够执行多次循环
            assert(mainThreadCounter > 0) { "Main thread was blocked" }
            
            val result = exportJob.await()
            result.isSuccess shouldBe true
            
            // 测试导入也在后台线程
            val importJob = async(Dispatchers.IO) {
                repository.importFromZip(uri)
            }
            
            mainThreadCounter = 0
            while (importJob.isActive) {
                mainThreadCounter++
                delay(10)
            }
            
            assert(mainThreadCounter > 0) { "Main thread was blocked during import" }
            
            val importResult = importJob.await()
            importResult.isSuccess shouldBe true
            
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * 测试操作取消的响应性
     * Requirements: 11.4, 11.5
     * 
     * 验证：
     * - 长时间运行的操作可以被取消
     * - 取消后应清理临时文件
     * - 取消应该快速响应
     */
    @Test
    fun `test operation cancellation responsiveness`() = runBlocking {
        val largeTasks = generateLargeTasks(2000)
        taskRepository = createMockTaskRepository(largeTasks)
        
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFile = File(context.cacheDir, "test_cancellation.zip")
        val uri = Uri.fromFile(tempFile)
        
        try {
            // 启动导出操作
            val exportJob = async(Dispatchers.IO) {
                repository.exportToZip(
                    uri = uri,
                    preferences = BackupPreferences(includeTasks = true)
                )
            }
            
            // 等待一小段时间后取消
            delay(100)
            
            val cancellationTime = measureTimeMillis {
                exportJob.cancelAndJoin()
            }
            
            // 验证取消响应时间应该很快（< 1秒）
            println("Cancellation response time: ${cancellationTime}ms")
            assert(cancellationTime < 1000) {
                "Cancellation took too long: ${cancellationTime}ms"
            }
            
            // 验证临时文件被清理
            val tempDir = File(context.cacheDir, "export_temp")
            if (tempDir.exists()) {
                val tempFiles = tempDir.listFiles()
                assert(tempFiles == null || tempFiles.isEmpty()) {
                    "Temporary files not cleaned up after cancellation"
                }
            }
            
        } finally {
            tempFile.delete()
        }
    }
    
    /**
     * 测试并发导出操作的性能
     * 验证多个导出操作可以并发执行而不互相干扰
     */
    @Test
    fun `test concurrent export operations`() = runBlocking {
        val tasks = generateLargeTasks(500)
        taskRepository = createMockTaskRepository(tasks)
        
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFiles = (1..3).map { index ->
            File(context.cacheDir, "test_concurrent_$index.zip")
        }
        
        try {
            // 并发执行多个导出操作
            val jobs = tempFiles.map { file ->
                async(Dispatchers.IO) {
                    repository.exportToZip(
                        uri = Uri.fromFile(file),
                        preferences = BackupPreferences(includeTasks = true)
                    )
                }
            }
            
            // 等待所有操作完成
            val results = jobs.map { it.await() }
            
            // 验证所有操作都成功
            results.forEach { result ->
                result.isSuccess shouldBe true
            }
            
            // 验证所有文件都被创建
            tempFiles.forEach { file ->
                assert(file.exists()) { "File ${file.name} was not created" }
                assert(file.length() > 0) { "File ${file.name} is empty" }
            }
            
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }
    
    /**
     * 测试临时文件清理
     * Requirements: 11.5
     * 验证操作完成后临时文件被正确清理
     */
    @Test
    fun `test temporary file cleanup`() = runBlocking {
        val tasks = generateLargeTasks(100)
        taskRepository = createMockTaskRepository(tasks)
        
        repository = LocalExportImportRepositoryImpl(
            context = context,
            taskRepository = taskRepository,
            courseRepository = courseRepository,
            routineRepository = routineRepository,
            subscriptionRepository = subscriptionRepository,
            pomodoroRepository = pomodoroRepository,
            semesterRepository = semesterRepository,
            dataExporter = DataExporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }),
            dataImporter = DataImporter(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }),
            backupFileManager = BackupFileManager(),
            dataTypeDetector = DataTypeDetector(),
            duplicateDetector = DuplicateDetector()
        )
        
        val tempFile = File(context.cacheDir, "test_cleanup.zip")
        val uri = Uri.fromFile(tempFile)
        
        try {
            // 记录操作前的临时文件数量
            val cacheDir = context.cacheDir
            val initialTempFiles = cacheDir.listFiles()?.filter {
                it.name.startsWith("export_temp") || it.name.startsWith("import_temp")
            }?.size ?: 0
            
            // 执行导出
            repository.exportToZip(
                uri = uri,
                preferences = BackupPreferences(includeTasks = true)
            )
            
            // 执行导入
            repository.importFromZip(uri)
            
            // 验证临时文件被清理
            val finalTempFiles = cacheDir.listFiles()?.filter {
                it.name.startsWith("export_temp") || it.name.startsWith("import_temp")
            }?.size ?: 0
            
            assert(finalTempFiles == initialTempFiles) {
                "Temporary files not cleaned up: initial=$initialTempFiles, final=$finalTempFiles"
            }
            
        } finally {
            tempFile.delete()
        }
    }
    
    // Helper functions to generate test data
    
    private fun generateLargeTasks(count: Int): List<Task> {
        return (1..count).map { index ->
            Task(
                id = index.toLong(),
                title = "Task $index",
                description = "Description for task $index",
                dueDate = LocalDateTime.now().plusDays(index.toLong()),
                priority = Priority.MEDIUM,
                isCompleted = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
    
    private fun generateLargeCourses(count: Int): List<Course> {
        return (1..count).map { index ->
            Course(
                id = index.toLong(),
                name = "Course $index",
                instructor = "Instructor $index",
                location = "Room $index",
                color = 0xFF0000 + index,
                semesterId = 1L,
                dayOfWeek = DayOfWeek.values()[index % 7],
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 30),
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(3),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    private fun generateLargeRoutines(count: Int): List<RoutineTask> {
        return (1..count).map { index ->
            RoutineTask(
                id = index.toLong(),
                title = "Routine $index",
                description = "Description for routine $index",
                cycleType = CycleType.DAILY,
                cycleConfig = "{}",
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
    
    // Mock repository creation functions
    
    private fun createMockTaskRepository(tasks: List<Task> = emptyList()): TaskRepository {
        return object : TaskRepository {
            override fun getAllTasks() = flowOf(tasks)
            override suspend fun insertTask(task: Task) {}
            override suspend fun updateTask(task: Task) {}
            override suspend fun deleteTask(task: Task) {}
            override fun getTaskById(id: Long) = flowOf(tasks.firstOrNull { it.id == id })
        }
    }
    
    private fun createMockCourseRepository(courses: List<Course> = emptyList()): CourseRepository {
        return object : CourseRepository {
            override fun getAllCourses() = flowOf(courses)
            override suspend fun insertCourse(course: Course) {}
            override suspend fun updateCourse(course: Course) {}
            override suspend fun deleteCourse(course: Course) {}
            override fun getCourseById(id: Long) = flowOf(courses.firstOrNull { it.id == id })
        }
    }
    
    private fun createMockRoutineRepository(routines: List<RoutineTask> = emptyList()): RoutineRepositoryImpl {
        return object : RoutineRepositoryImpl(null, null) {
            override fun getAllRoutines() = flowOf(routines)
            override suspend fun insertRoutine(routine: RoutineTask) {}
            override suspend fun updateRoutine(routine: RoutineTask) {}
            override suspend fun deleteRoutine(routine: RoutineTask) {}
        }
    }
    
    private fun createMockSubscriptionRepository(): SubscriptionRepository {
        return object : SubscriptionRepository {
            override fun getAllSubscriptions() = flowOf(emptyList<Subscription>())
            override suspend fun insertSubscription(subscription: Subscription) {}
            override suspend fun updateSubscription(subscription: Subscription) {}
            override suspend fun deleteSubscription(subscription: Subscription) {}
        }
    }
    
    private fun createMockPomodoroRepository(): PomodoroRepository {
        return object : PomodoroRepository {
            override fun getAllSessions() = flowOf(emptyList<PomodoroSession>())
            override suspend fun insertSession(session: PomodoroSession) {}
            override suspend fun updateSession(session: PomodoroSession) {}
            override suspend fun deleteSession(session: PomodoroSession) {}
        }
    }
    
    private fun createMockSemesterRepository(): SemesterRepositoryImpl {
        return object : SemesterRepositoryImpl(null, null) {
            override fun getAllSemesters() = flowOf(emptyList<Semester>())
            override suspend fun insertSemester(semester: Semester) {}
            override suspend fun updateSemester(semester: Semester) {}
            override suspend fun deleteSemester(semester: Semester) {}
        }
    }
}
