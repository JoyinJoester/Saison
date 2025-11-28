package takagi.ru.saison.util.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import takagi.ru.saison.domain.model.backup.DataType
import javax.inject.Inject

/**
 * 数据类型检测器
 * 通过分析 JSON 结构自动检测数据类型
 */
class DataTypeDetector @Inject constructor(
    private val json: Json
) {
    
    /**
     * 自动检测 JSON 文件的数据类型
     * @param jsonString JSON 字符串
     * @return 检测到的数据类型，如果无法检测则返回 null
     */
    fun detectDataType(jsonString: String): DataType? {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            
            // 检查是否为数组
            if (jsonElement !is JsonArray || jsonElement.isEmpty()) {
                return null
            }
            
            // 获取第一个对象
            val firstObject = jsonElement.firstOrNull()?.jsonObject ?: return null
            
            // 根据特征字段检测数据类型
            detectFromFields(firstObject)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun detectFromFields(obj: JsonObject): DataType? {
        return when {
            // 任务：包含 dueDate 和 isCompleted
            obj.containsKey("dueDate") && 
            obj.containsKey("isCompleted") && 
            obj.containsKey("priority") -> DataType.TASKS
            
            // 课程：包含 dayOfWeek 和 startTime
            obj.containsKey("dayOfWeek") && 
            obj.containsKey("startTime") && 
            obj.containsKey("semesterId") -> DataType.COURSES
            
            // 事件：包含 eventDate 和 category
            obj.containsKey("eventDate") && 
            obj.containsKey("category") && 
            obj.containsKey("reminderEnabled") -> DataType.EVENTS
            
            // 例行任务：包含 cycleType 和 cycleConfig
            obj.containsKey("cycleType") && 
            obj.containsKey("cycleConfig") && 
            obj.containsKey("isActive") -> DataType.ROUTINES
            
            // 订阅：包含 billingCycle 和 nextBillingDate
            obj.containsKey("billingCycle") && 
            obj.containsKey("nextBillingDate") && 
            obj.containsKey("price") -> DataType.SUBSCRIPTIONS
            
            // 番茄钟：包含 isBreak 和 isLongBreak
            obj.containsKey("isBreak") && 
            obj.containsKey("isLongBreak") && 
            obj.containsKey("duration") -> DataType.POMODORO_SESSIONS
            
            // 学期：包含 totalWeeks 和 isArchived
            obj.containsKey("totalWeeks") && 
            obj.containsKey("isArchived") && 
            obj.containsKey("isDefault") -> DataType.SEMESTERS
            
            else -> null
        }
    }
}
