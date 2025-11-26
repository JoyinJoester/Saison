package takagi.ru.saison.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("taskId"),
        Index("routineTaskId"),
        Index("startTime")
    ]
)
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val taskId: Long? = null,
    val routineTaskId: Long? = null,  // 关联的日程任务ID
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Int, // 计划时长（分钟）
    val actualDuration: Int? = null,  // 实际时长（分钟）
    
    val isCompleted: Boolean = false,
    val isBreak: Boolean = false,
    val isLongBreak: Boolean = false,
    val isEarlyFinish: Boolean = false,  // 是否提前结束
    
    val interruptions: Int = 0,
    val notes: String? = null,
    
    // 循环模式相关字段
    val sessionType: String = "WORK",  // "WORK", "SHORT_BREAK", "LONG_BREAK"
    val cycleIndex: Int? = null,  // 所属周期索引(从0开始)
    val sessionIndexInCycle: Int? = null  // 周期内会话索引(从0开始)
)
