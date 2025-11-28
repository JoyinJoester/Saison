package takagi.ru.saison.domain.usecase.local

import android.net.Uri
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.backup.ExportSummary
import javax.inject.Inject

/**
 * 导出为单个 JSON 文件的 Use Case
 */
class ExportToJsonUseCase @Inject constructor(
    private val repository: LocalExportImportRepository
) {
    suspend operator fun invoke(
        uri: Uri,
        dataType: DataType
    ): Result<ExportSummary> {
        return repository.exportToJson(uri, dataType)
    }
}
