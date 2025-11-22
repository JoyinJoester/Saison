package takagi.ru.saison.ui.screens.settings

/**
 * 导出模式
 */
enum class ExportMode {
    /** 导出当前学期 */
    CURRENT_SEMESTER,
    
    /** 导出所有学期 */
    ALL_SEMESTERS
}

/**
 * 导出选项
 */
data class ExportOptions(
    /** 导出模式 */
    val mode: ExportMode,
    
    /** 兼容模式（不包含完整配置） */
    val compatibilityMode: Boolean
)
