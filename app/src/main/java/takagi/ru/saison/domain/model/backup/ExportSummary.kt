package takagi.ru.saison.domain.model.backup

/**
 * 导出操作摘要
 */
data class ExportSummary(
    val totalItems: Int,
    val exportedTypes: List<DataType>,
    val filePath: String,
    val fileSize: Long
)
