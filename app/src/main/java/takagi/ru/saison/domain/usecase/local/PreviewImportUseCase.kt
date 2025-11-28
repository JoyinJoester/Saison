package takagi.ru.saison.domain.usecase.local

import android.net.Uri
import takagi.ru.saison.data.repository.local.LocalExportImportRepository
import takagi.ru.saison.domain.model.backup.ImportPreview
import javax.inject.Inject

/**
 * 预览导入内容的 Use Case
 */
class PreviewImportUseCase @Inject constructor(
    private val repository: LocalExportImportRepository
) {
    suspend operator fun invoke(uri: Uri): Result<ImportPreview> {
        return repository.previewImport(uri)
    }
}
