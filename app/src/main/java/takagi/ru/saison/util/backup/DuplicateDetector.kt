package takagi.ru.saison.util.backup

import takagi.ru.saison.domain.model.Course
import takagi.ru.saison.domain.model.PomodoroSession
import takagi.ru.saison.domain.model.Semester
import takagi.ru.saison.domain.model.Subscription
import takagi.ru.saison.domain.model.Task
import takagi.ru.saison.domain.model.routine.RoutineTask
import javax.inject.Inject

/**
 * 重复检测器
 * 用于检测导入数据中的重复项
 */
class DuplicateDetector @Inject constructor() {
    
    /**
     * 检测任务是否重复
     * 使用 ID 或（标题 + 截止日期 + 类别）作为唯一标识
     */
    fun isTaskDuplicate(task: Task, existing: List<Task>): Boolean {
        return existing.any { existingTask ->
            existingTask.id == task.id ||
            (existingTask.title == task.title &&
             existingTask.dueDate == task.dueDate &&
             existingTask.category?.id == task.category?.id)
        }
    }
    
    /**
     * 检测课程是否重复
     * 使用 ID 或（名称 + 学期）作为唯一标识
     */
    fun isCourseDuplicate(course: Course, existing: List<Course>): Boolean {
        return existing.any { existingCourse ->
            existingCourse.id == course.id ||
            (existingCourse.name == course.name &&
             existingCourse.semesterId == course.semesterId)
        }
    }
    
    /**
     * 检测例行任务是否重复
     * 使用 ID 或（名称 + 周期配置）作为唯一标识
     */
    fun isRoutineDuplicate(routine: RoutineTask, existing: List<RoutineTask>): Boolean {
        return existing.any { existingRoutine ->
            existingRoutine.id == routine.id ||
            (existingRoutine.title == routine.title &&
             existingRoutine.cycleConfig.toString() == routine.cycleConfig.toString())
        }
    }
    
    /**
     * 检测订阅是否重复
     * 使用 ID 或（名称 + 开始日期）作为唯一标识
     */
    fun isSubscriptionDuplicate(subscription: Subscription, existing: List<Subscription>): Boolean {
        return existing.any { existingSubscription ->
            existingSubscription.id == subscription.id ||
            (existingSubscription.name == subscription.name &&
             existingSubscription.startDate == subscription.startDate)
        }
    }
    
    /**
     * 检测番茄钟记录是否重复
     * 使用 ID 或（开始时间 + 时长 + 任务ID）作为唯一标识
     */
    fun isPomodoroDuplicate(session: PomodoroSession, existing: List<PomodoroSession>): Boolean {
        return existing.any { existingSession ->
            existingSession.id == session.id ||
            (existingSession.startTime == session.startTime &&
             existingSession.duration == session.duration &&
             existingSession.taskId == session.taskId)
        }
    }
    
    /**
     * 检测学期是否重复
     * 使用 ID 或（名称 + 开始日期 + 结束日期）作为唯一标识
     */
    fun isSemesterDuplicate(semester: Semester, existing: List<Semester>): Boolean {
        return existing.any { existingSemester ->
            existingSemester.id == semester.id ||
            (existingSemester.name == semester.name &&
             existingSemester.startDate == semester.startDate &&
             existingSemester.endDate == semester.endDate)
        }
    }
}
