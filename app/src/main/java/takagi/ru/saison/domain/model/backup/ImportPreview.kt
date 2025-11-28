package takagi.ru.saison.domain.model.backup

/**
 * 导入预览信息
 */
data class ImportPreview(
    val dataTypes: Map<DataType, Int>,
    val totalItems: Int,
    val newItems: Int,
    val duplicateItems: Int,
    val isZipFile: Boolean
)
