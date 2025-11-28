package takagi.ru.saison.domain.usecase.local

import android.net.Uri
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.domain.model.backup.RestoreSummary
import javax.inject.Inject

/**
 * 从 ZIP 文件导入的 Use Case
 */
class ImportFromZipUseCase @Inject constructor(
    private val repository: LocalExportImportRepository
) {
    suspend operator fun invoke(uri: Uri): Result<RestoreSummary> {
        return repository.importFromZip(uri)
    }
}
