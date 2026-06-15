# Saison 项目重构计划

## 目标
将当前混乱的代码结构重构为高可维护性的 Clean Architecture，降低技术债务。

---

## Phase 1: UI 层解耦 (预计 2-3 周)

### 1.1 拆分巨型 Screen 文件

#### 问题
- SettingsScreen.kt: 1664 行
- TaskListScreen.kt: 1015 行
- SubscriptionScreen.kt: 1000 行
- CourseSettingsSheet.kt: 1054 行

#### 重构策略
```
ui/screens/settings/
├── SettingsScreen.kt                 // 主入口 (50-100 行)
├── components/
│   ├── ThemeSettingsSection.kt      // 主题设置区块
│   ├── LanguageSettingsSection.kt   // 语言设置区块
│   ├── NotificationSection.kt       // 通知设置区块
│   ├── SecuritySection.kt           // 安全设置区块
│   ├── BackupSection.kt             // 备份设置区块
│   ├── AboutSection.kt              // 关于区块
│   └── dialogs/
│       ├── ThemeSelectionDialog.kt
│       ├── LanguageDialog.kt
│       └── AboutDialog.kt
└── SettingsViewModel.kt             // 保持不变
```

#### 拆分原则
- **单一职责**：每个 Composable 只负责一个功能区块
- **行数限制**：单个文件不超过 300 行
- **可测试性**：所有组件接受参数，便于 Preview 和测试

#### 执行步骤
1. 创建 `components/` 目录结构
2. 提取独立的 Section Composables
3. 提取 Dialog/BottomSheet 组件
4. 重构 SettingsScreen 为组合容器
5. 验证功能完整性

---

## Phase 2: 主题系统重构 (预计 1 周)

### 2.1 Color.kt 爆炸问题

#### 当前问题
- 726 个颜色常量（13主题 × 2模式 × 28色）
- 完全不可扩展，添加新主题需要手动定义 56 个常量

#### 目标架构
```kotlin
// ui/theme/model/ThemeColors.kt
data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color
) {
    fun toColorScheme(): ColorScheme = lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        // ... 映射所有字段
    )
}

// ui/theme/registry/ThemeRegistry.kt
object ThemeRegistry {
    private val themes = mapOf(
        SeasonalTheme.SAKURA to ThemeDefinition(
            light = ThemeColors(
                primary = Color(0xFFE91E63),
                onPrimary = Color(0xFFFFFFFF),
                // ... 28 个颜色
            ),
            dark = ThemeColors(
                primary = Color(0xFFFFB4AB),
                onPrimary = Color(0xFF690012),
                // ... 28 个颜色
            )
        ),
        SeasonalTheme.MINT to ThemeDefinition(/* ... */),
        // 其他主题...
    )
    
    fun getColors(theme: SeasonalTheme, isDark: Boolean): ThemeColors {
        return themes[theme]?.let { 
            if (isDark) it.dark else it.light 
        } ?: themes[SeasonalTheme.DYNAMIC]!!.light
    }
}

// ui/theme/Theme.kt (简化后)
@Composable
fun SaisonTheme(
    seasonalTheme: SeasonalTheme = SeasonalTheme.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = ThemeRegistry.getColors(seasonalTheme, darkTheme)
    val colorScheme = colors.toColorScheme()
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

#### 优势
- ✅ 添加新主题只需定义一个 `ThemeDefinition` 对象（56 行）
- ✅ 主题颜色可以序列化存储（支持用户自定义主题）
- ✅ 代码量从 780 行降到 ~200 行
- ✅ 类型安全，编译期检查

---

## Phase 3: 业务逻辑层解耦 (预计 2 周)

### 3.1 引入完整的 UseCase 层

#### 当前问题
- 只有 4 个 UseCase，大量业务逻辑在 ViewModel 中
- CourseViewModel: 792 行，混杂计算、验证、导出等逻辑

#### 目标结构
```
domain/usecase/
├── course/
│   ├── GetCoursesUseCase.kt
│   ├── AddCourseUseCase.kt
│   ├── UpdateCourseUseCase.kt
│   ├── DeleteCourseUseCase.kt
│   ├── CalculateWeekNumberUseCase.kt
│   ├── ExportCoursesToICSUseCase.kt
│   ├── ImportCoursesFromICSUseCase.kt
│   └── ValidateCourseDataUseCase.kt
├── task/
│   ├── GetTasksUseCase.kt
│   ├── CreateTaskUseCase.kt
│   ├── UpdateTaskUseCase.kt
│   ├── DeleteTaskUseCase.kt
│   ├── ParseNaturalLanguageTaskUseCase.kt
│   └── GetTaskStatisticsUseCase.kt
├── pomodoro/
│   ├── StartPomodoroUseCase.kt
│   ├── PausePomodoroUseCase.kt
│   ├── GetPomodoroStatisticsUseCase.kt
│   └── NotifyPomodoroCompletionUseCase.kt
└── subscription/
    ├── GetSubscriptionsUseCase.kt
    ├── CalculateNextRenewalUseCase.kt
    └── GetSubscriptionStatisticsUseCase.kt
```

#### UseCase 模板
```kotlin
// domain/usecase/base/UseCase.kt
interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}

// domain/usecase/course/GetCoursesUseCase.kt
class GetCoursesUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository
) : UseCase<GetCoursesUseCase.Params, Flow<List<Course>>> {
    
    data class Params(
        val semesterId: Long,
        val weekNumber: Int? = null,
        val dayOfWeek: DayOfWeek? = null
    )
    
    override suspend fun invoke(params: Params): Flow<List<Course>> {
        return courseRepository.getCoursesBySemester(params.semesterId)
            .map { courses ->
                courses.filter { course ->
                    // 业务逻辑：根据周次和星期筛选
                    val matchesWeek = params.weekNumber?.let { week ->
                        course.matchesWeek(week)
                    } ?: true
                    
                    val matchesDay = params.dayOfWeek?.let { day ->
                        course.dayOfWeek == day
                    } ?: true
                    
                    matchesWeek && matchesDay
                }
            }
    }
}
```

#### ViewModel 简化示例
```kotlin
// 重构前：CourseViewModel (792 行)
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val preferencesManager: PreferencesManager,
    private val weekCalculator: WeekCalculator,
    private val exportCourseDataUseCase: ExportCourseDataUseCase
) : ViewModel() {
    
    fun loadCourses() {
        viewModelScope.launch {
            val semesterId = preferencesManager.getCurrentSemesterId() ?: return@launch
            val semester = semesterRepository.getSemesterById(semesterId) ?: return@launch
            val weekNumber = calculateWeekNumber(semester.startDate)
            val courses = courseRepository.getCoursesBySemester(semesterId)
                .first()
                .filter { it.matchesWeek(weekNumber) }
            _uiState.value = CourseUiState.Success(courses)
        }
    }
    
    private fun calculateWeekNumber(startDate: LocalDate): Int {
        // 50 行计算逻辑...
    }
}

// 重构后：CourseViewModel (~300 行)
class CourseViewModel @Inject constructor(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val addCourseUseCase: AddCourseUseCase,
    private val updateCourseUseCase: UpdateCourseUseCase,
    private val deleteCourseUseCase: DeleteCourseUseCase,
    private val calculateWeekNumberUseCase: CalculateWeekNumberUseCase,
    private val exportCoursesUseCase: ExportCoursesToICSUseCase
) : ViewModel() {
    
    fun loadCourses(semesterId: Long) {
        viewModelScope.launch {
            val weekNumber = calculateWeekNumberUseCase()
            getCoursesUseCase(
                GetCoursesUseCase.Params(
                    semesterId = semesterId,
                    weekNumber = weekNumber
                )
            ).collect { courses ->
                _uiState.value = CourseUiState.Success(courses)
            }
        }
    }
}
```

---

## Phase 4: Repository 层优化 (预计 1 周)

### 4.1 引入 Result 封装

#### 当前问题
- 错误处理不统一，有的用异常，有的返回 null
- 没有统一的加载状态管理

#### 目标架构
```kotlin
// domain/model/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// domain/repository/CourseRepository.kt (接口)
interface CourseRepository {
    fun getCoursesBySemester(semesterId: Long): Flow<Result<List<Course>>>
    suspend fun addCourse(course: Course): Result<Long>
    suspend fun updateCourse(course: Course): Result<Unit>
    suspend fun deleteCourse(courseId: Long): Result<Unit>
}

// data/repository/CourseRepositoryImpl.kt (实现)
class CourseRepositoryImpl @Inject constructor(
    private val courseDao: CourseDao,
    private val ioDispatcher: CoroutineDispatcher
) : CourseRepository {
    
    override fun getCoursesBySemester(semesterId: Long): Flow<Result<List<Course>>> = flow {
        emit(Result.Loading)
        try {
            courseDao.getCoursesBySemesterId(semesterId)
                .map { entities -> entities.map { it.toDomain() } }
                .collect { courses ->
                    emit(Result.Success(courses))
                }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(ioDispatcher)
    
    override suspend fun addCourse(course: Course): Result<Long> = withContext(ioDispatcher) {
        try {
            val id = courseDao.insertCourse(course.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

---

## Phase 5: 测试覆盖 (预计 2 周)

### 5.1 单元测试

```
app/src/test/java/takagi/ru/saison/
├── domain/usecase/
│   ├── GetCoursesUseCaseTest.kt
│   ├── CalculateWeekNumberUseCaseTest.kt
│   └── ParseNaturalLanguageTaskUseCaseTest.kt
├── data/repository/
│   ├── CourseRepositoryImplTest.kt
│   └── TaskRepositoryImplTest.kt
└── ui/viewmodel/
    ├── CourseViewModelTest.kt
    └── TaskViewModelTest.kt
```

#### 测试示例
```kotlin
// domain/usecase/GetCoursesUseCaseTest.kt
@Test
fun `should filter courses by week number`() = runTest {
    // Given
    val courses = listOf(
        Course(id = 1, weekPattern = WeekPattern.ALL),
        Course(id = 2, weekPattern = WeekPattern.ODD)
    )
    val repository = FakeCourseRepository(courses)
    val useCase = GetCoursesUseCase(repository)
    
    // When
    val result = useCase(
        GetCoursesUseCase.Params(
            semesterId = 1L,
            weekNumber = 2 // 偶数周
        )
    ).first()
    
    // Then
    assertEquals(1, result.size)
    assertEquals(1, result[0].id)
}
```

### 5.2 UI 测试（Compose）

```kotlin
// ui/screens/course/CourseScreenTest.kt
@Test
fun courseScreen_displaysCoursesCorrectly() {
    composeTestRule.setContent {
        SaisonTheme {
            CourseScreen(
                uiState = CourseUiState.Success(
                    courses = testCourses,
                    currentWeek = 1
                ),
                onCourseClick = {}
            )
        }
    }
    
    composeTestRule
        .onNodeWithText("数学")
        .assertIsDisplayed()
}
```

---

## Phase 6: 性能优化 (预计 1 周)

### 6.1 减少重组

```kotlin
// 不好：每次 state 变化都重组整个 list
@Composable
fun CourseList(courses: List<Course>) {
    LazyColumn {
        items(courses) { course ->
            CourseCard(course = course, viewModel = viewModel)
        }
    }
}

// 好：使用 key 和 immutable 数据
@Composable
fun CourseList(
    courses: List<Course>,
    onCourseClick: (Course) -> Unit
) {
    LazyColumn {
        items(
            items = courses,
            key = { it.id }
        ) { course ->
            CourseCard(
                course = course,
                onClick = { onCourseClick(course) }
            )
        }
    }
}
```

### 6.2 使用 derivedStateOf

```kotlin
// 不好：每次 scroll 都触发 filter
val filteredCourses = courses.filter { /* ... */ }

// 好：只有 courses 或 filter 变化时才重新计算
val filteredCourses by remember(courses, filterCriteria) {
    derivedStateOf {
        courses.filter { /* ... */ }
    }
}
```

---

## 执行时间线

| Phase | 时间 | 优先级 | 依赖 |
|-------|------|--------|------|
| Phase 1: UI 解耦 | 2-3 周 | 🔥 极高 | 无 |
| Phase 2: 主题重构 | 1 周 | 🔥 极高 | 无 |
| Phase 3: UseCase 层 | 2 周 | 🟡 高 | Phase 1 |
| Phase 4: Repository 优化 | 1 周 | 🟡 高 | Phase 3 |
| Phase 5: 测试覆盖 | 2 周 | 🟢 中 | Phase 3, 4 |
| Phase 6: 性能优化 | 1 周 | 🟢 中 | Phase 1-4 |

**总计：9-10 周（2-2.5 个月）**

---

## 重构守则

### ✅ DO
- 每次只重构一个模块
- 重构前先写测试（防止破坏现有功能）
- 使用 Git 分支管理（`refactor/ui-decomposition`）
- 每个 Phase 完成后合并到 main
- 保持应用可运行状态

### ❌ DON'T
- 不要一次性重构所有代码
- 不要在没有测试的情况下重构核心逻辑
- 不要改变对外 API（Repository 接口）
- 不要引入新的第三方库（除非必要）

---

## 验收标准

### Phase 1 验收
- [ ] 所有 Screen 文件不超过 300 行
- [ ] UI 组件可独立预览
- [ ] 无功能回归

### Phase 2 验收
- [ ] Color.kt 代码量减少 70%+
- [ ] 添加新主题只需 50 行代码
- [ ] 所有主题显示正常

### Phase 3 验收
- [ ] UseCase 数量达到 20+
- [ ] ViewModel 平均代码量降低 60%
- [ ] 业务逻辑 100% 可测试

### Phase 4 验收
- [ ] 所有 Repository 返回 Result 封装
- [ ] 错误处理统一
- [ ] 无 null 崩溃

### Phase 5 验收
- [ ] UseCase 测试覆盖率 80%+
- [ ] Repository 测试覆盖率 70%+
- [ ] 关键 UI 流程有测试

### Phase 6 验收
- [ ] 列表滚动帧率 > 55 FPS
- [ ] 应用启动时间 < 2 秒
- [ ] 无内存泄漏

---

## 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 引入新 Bug | 高 | 高 | Phase 5 增加测试覆盖 |
| 重构时间超期 | 中 | 中 | 按 Phase 分阶段执行，可随时停止 |
| 团队抵触 | 低 | 中 | 展示重构收益，提供对比数据 |
| 合并冲突 | 中 | 低 | 频繁同步 main 分支 |

---

## 参考资料

- [Android Clean Architecture](https://developer.android.com/topic/architecture)
- [Jetpack Compose Best Practices](https://developer.android.com/jetpack/compose/performance)
- [Material Design 3 Theming](https://m3.material.io/styles/color/the-color-system/tokens)
