package takagi.ru.saison.ui.components.local

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
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
 * 导出操作卡片
 * 提供导出为 ZIP 和导出单个文件的按钮
 * 
 * @param onExportZip 导出为 ZIP 的回调
 * @param onExportJson 导出单个 JSON 的回调
 * @param isExporting 是否正在导出
 * @param lastExportTime 上次导出时间（可选）
 * @param modifier Modifier
 */
@Composable
fun ExportActionsCard(
    onExportZip: () -> Unit,
    onExportJson: () -> Unit,
    isExporting: Boolean,
    lastExportTime: Long? = null,
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
                text = stringResource(R.string.local_export_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 上次导出时间（如果有）
            if (lastExportTime != null && lastExportTime > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.local_export_last_time,
                        formatTime(lastExportTime)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 导出为 ZIP 按钮
            Button(
                onClick = onExportZip,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.local_export_as_zip),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 导出单个文件按钮
            OutlinedButton(
                onClick = onExportJson,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.local_export_as_json),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // 加载指示器
            if (isExporting) {
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
                        text = stringResource(R.string.local_export_in_progress),
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
