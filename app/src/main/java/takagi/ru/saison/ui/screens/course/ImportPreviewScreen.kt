package takagi.ru.saison.ui.screens.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.courseexport.CourseData
import takagi.ru.saison.domain.model.courseexport.SemesterExportData
import takagi.ru.saison.domain.usecase.ConflictInfo

/**
 * 导入预览界面
 * 显示将要导入的课程表数据，并允许用户配置导入选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    onNavigateBack: () -> Unit,
    onImportSuccess: (Long) -> Unit,
    viewModel: ImportPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.import_preview)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ImportPreviewUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ImportPreviewUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        onBack = onNavigateBack
                    )
                }
                is ImportPreviewUiState.Success -> {
                    ImportPreviewContent(
                        data = state.data,
                        conflicts = state.conflicts,
                        semesterName = state.semesterName,
                        applyPeriodSettings = state.applyPeriodSettings,
                        applyDisplaySettings = state.applyDisplaySettings,
                        isImporting = state.isImporting,
                        onSemesterNameChange = { viewModel.updateSemesterName(it) },
                        onApplyPeriodSettingsChange = { viewModel.updateApplyPeriodSettings(it) },
                        onApplyDisplaySettingsChange = { viewModel.updateApplyDisplaySettings(it) },
                        onConfirm = { viewModel.executeImport() },
                        onCancel = onNavigateBack
                    )
                }
            }
        }
    }
    
    // 监听导入成功事件
    LaunchedEffect(Unit) {
        viewModel.importSuccessEvent.collect { semesterId ->
            onImportSuccess(semesterId)
        }
    }
}

@Composable
private fun ImportPreviewContent(
    data: SemesterExportData,
    conflicts: ConflictInfo,
    semesterName: String,
    applyPeriodSettings: Boolean,
    applyDisplaySettings: Boolean,
    isImporting: Boolean,
    onSemesterNameChange: (String) -> Unit,
    onApplyPeriodSettingsChange: (Boolean) -> Unit,
    onApplyDisplaySettingsChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 学期信息
            item {
                SemesterInfoCard(
                    data = data,
                    semesterName = semesterName,
                    hasNameConflict = conflicts.hasNameConflict,
                    onSemesterNameChange = onSemesterNameChange
                )
            }
            
            // 节次设置
            item {
                PeriodSettingsCard(
                    data = data,
                    applySettings = applyPeriodSettings,
                    hasConflict = conflicts.hasPeriodSettingsConflict,
                    onApplySettingsChange = onApplyPeriodSettingsChange
                )
            }
            
            // 显示设置
            item {
                DisplaySettingsCard(
                    data = data,
                    applySettings = applyDisplaySettings,
                    hasConflict = conflicts.hasDisplaySettingsConflict,
                    onApplySettingsChange = onApplyDisplaySettingsChange
                )
            }
            
            // 课程列表
            item {
                CourseListCard(courses = data.courses)
            }
        }
        
        // 底部按钮
        Surface(
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isImporting
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = !isImporting && semesterName.isNotBlank()
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.confirm_import))
                    }
                }
            }
        }
    }
}

@Composable
private fun SemesterInfoCard(
    data: SemesterExportData,
    semesterName: String,
    hasNameConflict: Boolean,
    onSemesterNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.semester_info),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = semesterName,
                onValueChange = onSemesterNameChange,
                label = { Text(text = stringResource(R.string.semester_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = hasNameConflict,
                supportingText = if (hasNameConflict) {
                    { Text(text = stringResource(R.string.semester_name_conflict)) }
                } else null,
                trailingIcon = if (hasNameConflict) {
                    {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else null
            )
            
            InfoRow(
                label = stringResource(R.string.date_range),
                value = "${data.semesterInfo.startDate} - ${data.semesterInfo.endDate}"
            )
            
            InfoRow(
                label = stringResource(R.string.current_week),
                value = "${data.semesterInfo.currentWeek} / ${data.semesterInfo.totalWeeks}"
            )
        }
    }
}

@Composable
private fun PeriodSettingsCard(
    data: SemesterExportData,
    applySettings: Boolean,
    hasConflict: Boolean,
    onApplySettingsChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.period_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (hasConflict) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            InfoRow(
                label = stringResource(R.string.total_periods),
                value = data.periodSettings.totalPeriods.toString()
            )
            
            InfoRow(
                label = stringResource(R.string.period_duration),
                value = "${data.periodSettings.periodDurationMinutes} ${stringResource(R.string.minutes)}"
            )
            
            InfoRow(
                label = stringResource(R.string.break_duration),
                value = "${data.periodSettings.breakDurationMinutes} ${stringResource(R.string.minutes)}"
            )
            
            InfoRow(
                label = stringResource(R.string.first_period_start),
                value = data.periodSettings.firstPeriodStartTime
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.apply_period_settings),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = applySettings,
                    onCheckedChange = onApplySettingsChange
                )
            }
            
            if (hasConflict && applySettings) {
                Text(
                    text = stringResource(R.string.period_settings_will_be_overwritten),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DisplaySettingsCard(
    data: SemesterExportData,
    applySettings: Boolean,
    hasConflict: Boolean,
    onApplySettingsChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.display_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (hasConflict) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            InfoRow(
                label = stringResource(R.string.show_weekend),
                value = if (data.displaySettings.showWeekend) 
                    stringResource(R.string.yes) else stringResource(R.string.no)
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.apply_display_settings),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = applySettings,
                    onCheckedChange = onApplySettingsChange
                )
            }
        }
    }
}

@Composable
private fun CourseListCard(courses: List<CourseData>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.courses),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "${courses.size} ${stringResource(R.string.courses_count)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (courses.isNotEmpty()) {
                HorizontalDivider()
                
                courses.take(5).forEach { course ->
                    CoursePreviewItem(course = course)
                }
                
                if (courses.size > 5) {
                    Text(
                        text = stringResource(R.string.and_more_courses, courses.size - 5),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoursePreviewItem(course: CourseData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${course.teacher ?: ""} ${course.location ?: ""}".trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "${course.startTime}-${course.endTime}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
            
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}
