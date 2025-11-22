# Design Document

## Overview

全新的课程表导入导出系统，使用JSON格式存储完整的课程表配置。系统完全重写，不保留任何现有的ICS导入导出代码。确保用户A导出的课程表，用户B导入后看到的界面和设置完全一致。

## Architecture

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
├─────────────────────────────────────────────────────────────┤
│  SettingsScreen  │  ExportDialog  │  ImportPreviewScreen    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                         │
├─────────────────────────────────────────────────────────────┤
│  SettingsViewModel  │  ImportPreviewViewModel               │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Use Case Layer                          │
├─────────────────────────────────────────────────────────────┤
│  ExportCourseDataUseCase  │  ImportCourseDataUseCase        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data/Repository Layer                     │
├─────────────────────────────────────────────────────────────┤
│  CourseRepository  │  SemesterRepository  │  PreferencesManager│
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Data Models

#### CourseExportData (全新数据模型)

```kotlin
@Serializable
data class CourseExportData(
    val metadata: ExportMetadata,
    val semesters: List<SemesterExportData>
)

@Serializable
data class ExportMetadata(
    val version: String = "1.0",
    val exportTime: Long,
    val appVersion: String,
    val deviceInfo: String
)

@Serializable
data class SemesterExportData(
    val semesterInfo: SemesterInfo,
    val periodSettings: PeriodSettingsData,
    val displaySettings: DisplaySettingsData,
    val courses: List<CourseData>
)

@Serializable
data class SemesterInfo(
    val name: String,
    val startDate: String, // ISO 8601 format
    val endDate: String,
    val currentWeek: Int,
    val totalWeeks: Int
)

@Serializable
data class PeriodSettingsData(
    val totalPeriods: Int,
    val periodDurationMinutes: Int,
    val breakDurationMinutes: Int,
    val firstPeriodStartTime: String, // HH:mm format
    val lunchBreakAfterPeriod: Int?,
    val lunchBreakDurationMinutes: Int?
)

@Serializable
data class DisplaySettingsData(
    val showWeekend: Boolean,
    val timeFormat24Hour: Boolean,
    val showPeriodNumber: Boolean,
    val compactMode: Boolean
)

@Serializable
data class CourseData(
    val name: String,
    val teacher: String?,
    val location: String?,
    val dayOfWeek: Int, // 1-7
    val startPeriod: Int,
    val endPeriod: Int,
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val weekPattern: WeekPatternData,
    val color: String, // Hex color
    val notes: String?
)

@Serializable
data class WeekPatternData(
    val type: String, // "ALL", "ODD", "EVEN", "CUSTOM"
    val customWeeks: List<Int>? // For CUSTOM type
)
```

### 2. Use Cases

#### ExportCourseDataUseCase (全新实现)

```kotlin
class ExportCourseDataUseCase(
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) {
    suspend fun exportToUri(
        uri: Uri,
        semesterIds: List<Long>
    ): Result<Unit>
    
    suspend fun collectExportData(
        semesterIds: List<Long>
    ): CourseExportData
    
    fun generateSuggestedFileName(
        semesterName: String
    ): String
}
```

#### ImportCourseDataUseCase (全新实现)

```kotlin
class ImportCourseDataUseCase(
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context
) {
    suspend fun parseFromUri(
        uri: Uri
    ): Result<CourseExportData>
    
    suspend fun validateImportData(
        data: CourseExportData
    ): ValidationResult
    
    suspend fun executeImport(
        data: SemesterExportData,
        options: ImportOptions
    ): Result<ImportResult>
    
    suspend fun detectConflicts(
        data: SemesterExportData
    ): ConflictInfo
}
```

### 3. UI Components

#### ExportDialog (全新组件)

```kotlin
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit
)

data class ExportOptions(
    val exportMode: ExportMode, // CURRENT_SEMESTER, ALL_SEMESTERS
    val selectedSemesterIds: List<Long>
)
```

#### ImportPreviewScreen (全新组件)

```kotlin
@Composable
fun ImportPreviewScreen(
    importData: CourseExportData,
    onConfirm: (ImportOptions) -> Unit,
    onCancel: () -> Unit
)

data class ImportOptions(
    val semesterName: String,
    val applyPeriodSettings: Boolean,
    val applyDisplaySettings: Boolean,
    val conflictResolution: ConflictResolution
)
```

### 4. ViewModels

#### SettingsViewModel (重构导入导出部分)

```kotlin
class SettingsViewModel {
    // Export
    fun prepareExport(options: ExportOptions)
    suspend fun executeExportToUri(uri: Uri): Result<Unit>
    fun getSuggestedFileName(): String
    
    // Import
    fun startImport(uri: Uri)
    
    // States
    val exportState: StateFlow<ExportState>
    val importState: StateFlow<ImportState>
}
```

#### ImportPreviewViewModel (全新ViewModel)

```kotlin
class ImportPreviewViewModel {
    fun loadImportData(uri: Uri)
    fun updateImportOptions(options: ImportOptions)
    fun executeImport()
    
    val importData: StateFlow<CourseExportData?>
    val conflicts: StateFlow<ConflictInfo>
    val importProgress: StateFlow<ImportProgress>
}
```

## Data Models

### JSON Structure Example

```json
{
  "metadata": {
    "version": "1.0",
    "exportTime": 1700000000000,
    "appVersion": "1.0.0",
    "deviceInfo": "Android 14"
  },
  "semesters": [
    {
      "semesterInfo": {
        "name": "2024-2025学年第一学期",
        "startDate": "2024-09-01",
        "endDate": "2025-01-15",
        "currentWeek": 10,
        "totalWeeks": 20
      },
      "periodSettings": {
        "totalPeriods": 12,
        "periodDurationMinutes": 45,
        "breakDurationMinutes": 10,
        "firstPeriodStartTime": "08:00",
        "lunchBreakAfterPeriod": 4,
        "lunchBreakDurationMinutes": 90
      },
      "displaySettings": {
        "showWeekend": false,
        "timeFormat24Hour": true,
        "showPeriodNumber": true,
        "compactMode": false
      },
      "courses": [
        {
          "name": "高等数学",
          "teacher": "张教授",
          "location": "教学楼A101",
          "dayOfWeek": 1,
          "startPeriod": 1,
          "endPeriod": 2,
          "startTime": "08:00",
          "endTime": "09:35",
          "weekPattern": {
            "type": "ALL",
            "customWeeks": null
          },
          "color": "#FF6200EE",
          "notes": "需要带计算器"
        }
      ]
    }
  ]
}
```

## Error Handling

### Export Errors

1. **No Semester Found**: 提示用户没有可导出的学期
2. **File Write Error**: 提示用户文件写入失败，检查存储权限
3. **Data Collection Error**: 提示用户数据收集失败，记录详细错误

### Import Errors

1. **Invalid JSON Format**: 提示用户文件格式无效
2. **Version Incompatible**: 提示用户版本不兼容
3. **Missing Required Fields**: 提示用户缺少必需字段
4. **File Read Error**: 提示用户文件读取失败
5. **Database Insert Error**: 提示用户数据导入失败，回滚操作

## Testing Strategy

### Unit Tests

1. **ExportCourseDataUseCase Tests**
   - 测试数据收集逻辑
   - 测试JSON序列化
   - 测试文件名生成

2. **ImportCourseDataUseCase Tests**
   - 测试JSON解析
   - 测试数据验证
   - 测试冲突检测
   - 测试导入逻辑

3. **Data Model Tests**
   - 测试序列化和反序列化
   - 测试数据转换

### Integration Tests

1. **Export Flow Test**
   - 测试完整导出流程
   - 验证导出的JSON格式

2. **Import Flow Test**
   - 测试完整导入流程
   - 验证导入后的数据一致性

3. **Round-trip Test**
   - 导出后立即导入
   - 验证数据完全一致

## Implementation Notes

### 关键实现点

1. **完全重写**：不保留任何现有的ICS导入导出代码
2. **JSON格式**：使用kotlinx.serialization进行序列化
3. **SAF集成**：使用ActivityResultContracts.CreateDocument和OpenDocument
4. **数据完整性**：确保导出所有必要信息，导入后完全一致
5. **用户体验**：提供清晰的预览和进度提示
6. **错误处理**：完善的错误提示和回滚机制

### 文件操作

```kotlin
// Export
val createDocument = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri?.let { viewModel.executeExportToUri(it) }
}

// Import
val openDocument = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { viewModel.startImport(it) }
}
```

### 数据转换

```kotlin
// Domain Model -> Export Data
fun Semester.toSemesterInfo(): SemesterInfo
fun CourseSettings.toPeriodSettingsData(): PeriodSettingsData
fun Course.toCourseData(): CourseData

// Export Data -> Domain Model
fun SemesterInfo.toSemester(): Semester
fun PeriodSettingsData.toCourseSettings(): CourseSettings
fun CourseData.toCourse(semesterId: Long): Course
```

## Migration Strategy

### 删除旧代码

1. 删除所有ICS相关的导入导出代码
2. 删除EnhancedIcsImportUseCase
3. 删除EnhancedIcsExportUseCase
4. 删除EnhancedIcsParser
5. 删除EnhancedIcsGenerator
6. 删除所有export包下的旧模型

### 保留的代码

1. CourseRepository（用于读取和写入课程数据）
2. SemesterRepository（用于读取和写入学期数据）
3. PreferencesManager（用于读取和写入设置）
4. 基础的Course和Semester模型

## UI Flow

### Export Flow

```
Settings Screen
    ↓ (点击导出)
Export Dialog
    ↓ (选择选项)
File Picker (SAF)
    ↓ (选择位置)
Exporting...
    ↓
Success Toast
```

### Import Flow

```
Settings Screen
    ↓ (点击导入)
File Picker (SAF)
    ↓ (选择文件)
Parsing...
    ↓
Import Preview Screen
    ↓ (确认)
Importing...
    ↓
Success Toast + Navigate to Semester
```

## Performance Considerations

1. **异步操作**：所有文件IO和数据库操作在IO线程执行
2. **进度提示**：长时间操作显示进度指示器
3. **内存管理**：大文件分块读取，避免OOM
4. **事务处理**：导入使用数据库事务，失败时回滚

## Security Considerations

1. **数据验证**：严格验证导入的JSON格式和数据
2. **权限检查**：使用SAF，无需额外存储权限
3. **错误处理**：防止恶意JSON导致崩溃
4. **数据隔离**：导入的数据创建新学期，不影响现有数据
