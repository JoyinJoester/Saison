package takagi.ru.saison.ui.components.local

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import takagi.ru.saison.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导入操作卡片
 * 提供从 ZIP 和从 JSON 导入的按钮
 * 
 * @param onImportZip 从 ZIP 导入的回调
 * @param onImportJson 从 JSON 导入的回调
 * @param isImporting 是否正在导入
 * @param lastImportTime 上次导入时间（可选）
 * @param modifier Modifier
 */
@Composable
fun ImportActionsCard(
    onImportZip: () -> Unit,
    onImportJson: () -> Unit,
    isImporting: Boolean,
    lastImportTime: Long? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.local_import_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 上次导入时间（如果有）
            if (lastImportTime != null && lastImportTime > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.local_import_last_time,
                        formatTime(lastImportTime)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 从 ZIP 导入按钮
            Button(
                onClick = onImportZip,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImporting,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.local_import_from_zip),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 从 JSON 导入按钮
            OutlinedButton(
                onClick = onImportJson,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImporting,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.local_import_from_json),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // 加载指示器
            if (isImporting) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.local_import_in_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
