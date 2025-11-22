package takagi.ru.saison.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import takagi.ru.saison.data.local.datastore.PreferencesManager
import takagi.ru.saison.data.repository.CourseRepository
import takagi.ru.saison.data.repository.SemesterRepository
import takagi.ru.saison.domain.model.courseexport.CourseExportData
import takagi.ru.saison.domain.model.courseexport.ExportMetadata
import takagi.ru.saison.domain.model.courseexport.SemesterExportData
import takagi.ru.saison.domain.model.courseexport.toCourseData
import takagi.ru.saison.domain.model.courseexport.toDisplaySettingsData
import takagi.ru.saison.domain.model.courseexport.toPeriodSettingsData
import takagi.ru.saison.domain.model.courseexport.toSemesterInfo
import takagi.ru.saison.util.WeekCalculator
import java.time.LocalDate
import javax.inject.Inject

/**
 * 导出课程表数据的Use Case
 * 负责收集所有必要的数据并导出为JSON文件
 */
class ExportCourseDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val preferencesManager: PreferencesManager,
    private val weekCalculator: WeekCalculator
) {
    
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    
    /**
     * 收集导出数据
     * @param semesterIds 要导出的学期ID列表
     * @return 完整的导出数据对象
     */
    suspend fun collectExportData(semesterIds: List<Long>): Result<CourseExportData> = withContext(Dispatchers.IO) {
        try {
            // 创建元数据
            val metadata = ExportMetadata(
                version = "1.0",
                exportTime = System.currentTimeMillis(),
                appVersion = getAppVersion(),
                deviceInfo = getDeviceInfo()
            )
            
            // 收集每个学期的数据
            val semestersData = semesterIds.mapNotNull { semesterId ->
                collectSemesterData(semesterId)
            }
            
            if (semestersData.isEmpty()) {
                return@withContext Result.failure(Exception("没有可导出的学期数据"))
            }
            
            val exportData = CourseExportData(
                metadata = metadata,
                semesters = semestersData
            )
            
            Result.success(exportData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 导出数据到指定Uri
     * @param uri 目标文件Uri
     * @param semesterIds 要导出的学期ID列表
     */
    suspend fun exportToUri(uri: Uri, semesterIds: List<Long>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 收集数据
            val exportDataResult = collectExportData(semesterIds)
            if (exportDataResult.isFailure) {
                return@withContext Result.failure(exportDataResult.exceptionOrNull()!!)
            }
            
            val exportData = exportDataResult.getOrThrow()
            
            // 序列化为JSON
            val jsonString = json.encodeToString(exportData)
            
            // 写入文件
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: return@withContext Result.failure(Exception("无法打开输出流"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 生成建议的文件名
     * @param semesterName 学期名称
     * @return 建议的文件名
     */
    fun generateSuggestedFileName(semesterName: String): String {
        val timestamp = System.currentTimeMillis()
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return "${semesterName}_${date}.json"
    }
    
    /**
     * 收集单个学期的数据
     */
    private suspend fun collectSemesterData(semesterId: Long): SemesterExportData? {
        // 获取学期信息
        val semester = semesterRepository.getSemesterByIdSync(semesterId) ?: return null
        
        // 获取课程设置
        val courseSettings = preferencesManager.courseSettings.first()
        
        // 获取课程列表
        val courses = courseRepository.getCoursesBySemester(semesterId).first()
        
        // 计算当前周数
        val currentWeek = weekCalculator.calculateCurrentWeek(
            semesterStartDate = semester.startDate,
            currentDate = LocalDate.now()
        )
        
        return SemesterExportData(
            semesterInfo = semester.toSemesterInfo(currentWeek),
            periodSettings = courseSettings.toPeriodSettingsData(),
            displaySettings = courseSettings.toDisplaySettingsData(),
            courses = courses.map { it.toCourseData() }
        )
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getDeviceInfo(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}
