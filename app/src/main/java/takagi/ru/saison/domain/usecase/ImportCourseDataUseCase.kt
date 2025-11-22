package takagi.ru.saison.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import takagi.ru.saison.data.local.datastore.PreferencesManager
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.SemesterRepository
import takagi.ru.saison.domain.model.courseexport.CourseExportData
import takagi.ru.saison.domain.model.courseexport.SemesterExportData
import takagi.ru.saison.domain.model.courseexport.applyCourseSettings
import takagi.ru.saison.domain.model.courseexport.toCourse
import takagi.ru.saison.domain.model.courseexport.toSemester
import javax.inject.Inject

/**
 * 导入课程表数据的Use Case
 * 负责解析JSON文件并导入数据到数据库
 */
class ImportCourseDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val preferencesManager: PreferencesManager
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * 从Uri解析导入数据
     * @param uri 源文件Uri
     * @return 解析后的导出数据
     */
    suspend fun parseFromUri(uri: Uri): Result<CourseExportData> = withContext(Dispatchers.IO) {
        try {
            // 读取文件内容
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(Exception("无法读取文件"))
            
            // 解析JSON
            val exportData = json.decodeFromString<CourseExportData>(jsonString)
            
            // 验证数据
            val validationResult = validateImportData(exportData)
            if (validationResult.isFailure) {
                return@withContext validationResult
            }
            
            Result.success(exportData)
        } catch (e: SerializationException) {
            Result.failure(Exception("文件格式无效：${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("解析失败：${e.message}"))
        }
    }
    
    /**
     * 验证导入数据
     */
    suspend fun validateImportData(data: CourseExportData): Result<CourseExportData> {
        try {
            // 检查版本号
            if (data.metadata.version != "1.0") {
                return Result.failure(Exception("不支持的版本：${data.metadata.version}"))
            }
            
            // 检查是否有学期数据
            if (data.semesters.isEmpty()) {
                return Result.failure(Exception("文件中没有学期数据"))
            }
            
            // 验证每个学期的数据
            data.semesters.forEach { semester ->
                if (semester.semesterInfo.name.isBlank()) {
                    return Result.failure(Exception("学期名称不能为空"))
                }
                if (semester.semesterInfo.totalWeeks <= 0) {
                    return Result.failure(Exception("学期周数必须大于0"))
                }
            }
            
            return Result.success(data)
        } catch (e: Exception) {
            return Result.failure(Exception("数据验证失败：${e.message}"))
        }
    }
    
    /**
     * 检测冲突
     * @param data 要导入的学期数据
     * @return 冲突信息
     */
    suspend fun detectConflicts(data: SemesterExportData): ConflictInfo = withContext(Dispatchers.IO) {
        val existingSemesters = semesterRepository.getAllSemesters().first()
        val nameConflict = existingSemesters.any { it.name == data.semesterInfo.name }
        
        val currentSettings = preferencesManager.courseSettings.first()
        val periodSettingsConflict = 
            currentSettings.totalPeriods != data.periodSettings.totalPeriods ||
            currentSettings.periodDuration != data.periodSettings.periodDurationMinutes ||
            currentSettings.breakDuration != data.periodSettings.breakDurationMinutes
        
        val displaySettingsConflict = 
            currentSettings.showWeekends != data.displaySettings.showWeekend
        
        ConflictInfo(
            hasNameConflict = nameConflict,
            hasPeriodSettingsConflict = periodSettingsConflict,
            hasDisplaySettingsConflict = displaySettingsConflict,
            existingSemesterName = if (nameConflict) data.semesterInfo.name else null
        )
    }
    
    /**
     * 执行导入
     * @param data 要导入的学期数据
     * @param options 导入选项
     * @return 导入结果
     */
    suspend fun executeImport(
        data: SemesterExportData,
        options: ImportOptions
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            // 创建学期
            val semester = data.semesterInfo.toSemester().copy(
                name = options.semesterName
            )
            val semesterId = semesterRepository.insertSemester(semester)
            
            // 应用节次设置（如果用户选择）
            if (options.applyPeriodSettings) {
                val currentSettings = preferencesManager.courseSettings.first()
                val newSettings = data.periodSettings.applyCourseSettings(currentSettings)
                preferencesManager.setCourseSettings(newSettings)
            }
            
            // 应用显示设置（如果用户选择）
            if (options.applyDisplaySettings) {
                val currentSettings = preferencesManager.courseSettings.first()
                val newSettings = data.displaySettings.applyCourseSettings(currentSettings)
                preferencesManager.setCourseSettings(newSettings)
            }
            
            // 导入课程
            val courses = data.courses.map { courseData ->
                courseData.toCourse(
                    semesterId = semesterId,
                    semesterStartDate = semester.startDate,
                    semesterEndDate = semester.endDate
                )
            }
            courseRepository.insertCourses(courses)
            
            Result.success(
                ImportResult(
                    semesterId = semesterId,
                    semesterName = options.semesterName,
                    courseCount = courses.size
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("导入失败：${e.message}"))
        }
    }
}

/**
 * 冲突信息
 */
data class ConflictInfo(
    val hasNameConflict: Boolean,
    val hasPeriodSettingsConflict: Boolean,
    val hasDisplaySettingsConflict: Boolean,
    val existingSemesterName: String?
)

/**
 * 导入选项
 */
data class ImportOptions(
    val semesterName: String,
    val applyPeriodSettings: Boolean,
    val applyDisplaySettings: Boolean
)

/**
 * 导入结果
 */
data class ImportResult(
    val semesterId: Long,
    val semesterName: String,
    val courseCount: Int
)
