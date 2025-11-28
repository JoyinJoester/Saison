package takagi.ru.saison.ui.screens.plus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.ui.components.PlusFeatureCard

/**
 * Saison Plus功能介绍和管理界面
 * 
 * @param onNavigateBack 返回回调
 * @param onNavigateToPayment 导航到付款页面回调
 * @param viewModel ViewModel
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaisonPlusScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPayment: () -> Unit,
    viewModel: SaisonPlusViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlusActivated by viewModel.isPlusActivated.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plus_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header - Plus状态
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlusActivated) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // 标题和描述
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.plus_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlusActivated) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.plus_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isPlusActivated) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Plus开关 - 只在激活后显示
                        if (isPlusActivated) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.plus_activation_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.plus_status_activated),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Switch(
                                    checked = true,
                                    onCheckedChange = { checked ->
                                        if (!checked) {
                                            viewModel.deactivatePlus()
                                        }
                                    },
                                    enabled = !uiState.isLoading
                                )
                            }
                        } else {
                            // 未激活时显示付款按钮
                            OutlinedButton(
                                onClick = onNavigateToPayment,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.plus_payment_button))
                            }
                        }
                    }
                }
            }
            
            // 功能列表标题
            item {
                Text(
                    text = stringResource(R.string.plus_features_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // 功能列表
            items(
                items = viewModel.getDisplayFeatures(uiState.features),
                key = { it.feature.id }
            ) { displayFeature ->
                PlusFeatureCard(
                    feature = displayFeature.feature,
                    isUnlocked = displayFeature.isUnlocked
                )
            }
            
            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 可以在这里显示Snackbar
            viewModel.clearError()
        }
    }
}
