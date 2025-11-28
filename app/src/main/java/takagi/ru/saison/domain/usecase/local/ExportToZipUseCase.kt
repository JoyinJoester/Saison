package takagi.ru.saison.domain.usecase.local

import android.net.Uri
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.domain.model.backup.BackupPreferences
import takagi.ru.saison.domain.model.backup.ExportSummary
import javax.inject.Inject

/**
 * 导出为 ZIP 文件的 Use Case
 */
class ExportToZipUseCase @Inject constructor(
    private val repository: LocalExportImportRepository
) {
    suspend operator fun invoke(
        uri: Uri,
        preferences: BackupPreferences
    ): Result<ExportSummary> {
        return repository.exportToZip(uri, preferences)
    }
}
