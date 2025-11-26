package takagi.ru.saison.data.repository

/**
 * 默认学期初始化器接口
 * 负责在应用启动时确保默认学期存在
 * Requirements: 2.1, 2.2, 2.3, 2.4
 */
interface DefaultSemesterInitializer {
    /**
     * 确保数据库中至少存在一个学期
     * 如果不存在，创建默认学期并关联孤立课程
     * Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4
     */
    suspend fun ensureDefaultSemester()
}
