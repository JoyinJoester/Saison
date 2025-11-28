package takagi.ru.saison.domain.usecase.local

import android.net.Uri
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.domain.model.backup.DataType
import takagi.ru.saison.domain.model.backup.RestoreSummary
import javax.inject.Inject

/**
 * 从 JSON 文件导入的 Use Case
 */
class ImportFromJsonUseCase @Inject constructor(
    private val repository: LocalExportImportRepository
) {
    suspend operator fun invoke(
        uri: Uri,
        dataType: DataType? = null
    ): Result<RestoreSummary> {
        return repository.importFromJson(uri, dataType)
    }
}
