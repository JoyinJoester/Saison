package takagi.ru.saison.ui.screens.subscription

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.data.local.database.entities.SubscriptionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    subscriptionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionDetailViewModel = hiltViewModel()
) {
    val subscription by viewModel.subscription.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenewalDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subscription?.name ?: stringResource(R.string.subscription_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.subscription_action_delete))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (uiState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                subscription?.let { sub ->
                    statistics?.let { stats ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            item {
                                DetailHeader(subscription = sub)
                            }
                            
                            // Auto-renewal toggle
                            item {
                                AutoRenewalCard(
                                    autoRenewal = sub.autoRenewal,
                                    onToggle = { viewModel.toggleAutoRenewal() }
                                )
                            }
                            
                            // Statistics
                            item {
                                StatisticsSection(
                                    statistics = stats,
                                    currency = sub.currency
                                )
                            }
                            
                            // Renewal section
                            item {
                                RenewalSection(
                                    subscription = sub,
                                    statistics = stats,
                                    onRenewClick = { showRenewalDialog = true }
                                )
                            }
                            
                            // Subscription info
                            item {
                                SubscriptionInfoCard(subscription = sub)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.subscription_action_delete)) },
            text = { Text(stringResource(R.string.subscription_delete_confirm_message, subscription?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubscription {
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.subscription_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.subscription_cancel_button))
                }
            }
        )
    }
    
    // Manual renewal dialog
    if (showRenewalDialog) {
        val renewalOptions by viewModel.renewalOptions.collectAsState()
        ManualRenewalDialog(
            subscription = subscription,
            renewalOptions = renewalOptions,
            onDismiss = { showRenewalDialog = false },
            onConfirm = { count ->
                viewModel.performManualRenewal(count)
                showRenewalDialog = false
            }
        )
    }
}

@Composable
private fun DetailHeader(subscription: SubscriptionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Subscriptions,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = subscription.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subscription.category,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AutoRenewalCard(
    autoRenewal: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.subscription_auto_renewal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (autoRenewal) {
                        stringResource(R.string.subscription_auto_renewal_enabled_desc)
                    } else {
                        stringResource(R.string.subscription_auto_renewal_disabled_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoRenewal,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun StatisticsSection(
    statistics: SubscriptionStatistics,
    currency: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.subscription_statistics_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                label = stringResource(R.string.subscription_accumulated_cost),
                value = "¥${String.format("%.2f", statistics.accumulatedCost)}"
            )
            StatisticCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.subscription_accumulated_duration),
                value = statistics.accumulatedDuration
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatisticCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TrendingUp,
                label = stringResource(R.string.subscription_average_monthly),
                value = "¥${String.format("%.2f", statistics.averageMonthlyCost)}"
            )
            StatisticCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarToday,
                label = stringResource(R.string.subscription_average_daily),
                value = "¥${String.format("%.2f", statistics.averageDailyCost)}"
            )
        }
    }
}

@Composable
private fun StatisticCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RenewalSection(
    subscription: SubscriptionEntity,
    statistics: SubscriptionStatistics,
    onRenewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_renewal_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val nextRenewalDate = Instant.ofEpochMilli(subscription.nextRenewalDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.subscription_next_renewal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = nextRenewalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (statistics.isOverdue) {
                        Text(
                            text = stringResource(R.string.subscription_overdue_days, -statistics.daysUntilRenewal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.subscription_days_until_renewal, statistics.daysUntilRenewal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!subscription.autoRenewal) {
                    Button(
                        onClick = onRenewClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.subscription_manual_renew))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionInfoCard(subscription: SubscriptionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            InfoRow(
                label = stringResource(R.string.subscription_price),
                value = "¥${String.format("%.2f", subscription.price)}"
            )
            
            InfoRow(
                label = stringResource(R.string.subscription_cycle_type),
                value = getCycleTypeText(subscription.cycleType)
            )
            
            val startDate = Instant.ofEpochMilli(subscription.startDate)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            InfoRow(
                label = stringResource(R.string.subscription_start_date),
                value = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )
            
            if (!subscription.note.isNullOrBlank()) {
                Divider()
                Text(
                    text = stringResource(R.string.subscription_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subscription.note,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}


