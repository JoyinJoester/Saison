# Implementation Plan

- [x] 1. 删除旧的导入导出代码















  - 删除所有ICS相关的导入导出文件和代码
  - _Requirements: 所有需求_




- [x] 1.1 删除旧的Use Case文件
  - 删除 `EnhancedIcsImportUseCase.kt`
  - 删除 `EnhancedIcsExportUseCase.kt`
  - 删除 `IcsImportUseCase.kt`
  - 删除 `IcsExportUseCase.kt`
  - _Requirements: 所有需求_


- [x] 1.2 删除旧的数据处理文件

  - 删除 `EnhancedIcsParser.kt`
  - 删除 `EnhancedIcsGenerator.kt`
  - 删除 `IcsParser.kt`
  - 删除 `IcsGenerator.kt`
  - _Requirements: 所有需求_


- [x] 1.3 删除旧的数据模型

  - 删除 `domain/model/export/` 目录下的所有文件
  - 删除 `ExportExtensions.kt`
  - 删除 `CourseExportPackage.kt`
  - _Requirements: 所有需求_


- [x] 1.4 删除旧的UI组件

  - 删除 `ExportCoursesDialog.kt`（如果存在）
  - 删除 `ImportPreviewScreen.kt`（旧版本）
  - 删除 `ImportPreviewViewModel.kt`（旧版本）
  - _Requirements: 所有需求_

- [x] 2. 创建新的数据模型


  - 创建JSON序列化的数据模型，包含所有课程表配置信息
  - _Requirements: 1.3, 1.4, 1.5, 1.6, 1.7, 8.1-8.8, 10.1-10.9_


- [x] 2.1 创建导出数据模型

  - 创建 `domain/model/courseexport/CourseExportData.kt`
  - 定义 `CourseExportData`, `ExportMetadata`, `SemesterExportData`
  - 添加 `@Serializable` 注解
  - _Requirements: 1.3, 1.4, 1.5, 1.6, 1.7, 8.1-8.6_


- [x] 2.2 创建学期和设置数据模型

  - 在同一文件中定义 `SemesterInfo`, `PeriodSettingsData`, `DisplaySettingsData`
  - 确保包含所有必要字段（开始日期、结束日期、当前周数、节次设置等）
  - _Requirements: 1.4, 1.5, 3.1-3.10, 10.1-10.3_


- [x] 2.3 创建课程数据模型

  - 在同一文件中定义 `CourseData`, `WeekPatternData`
  - 包含课程名称、教师、地点、时间、周数模式、颜色等所有信息
  - _Requirements: 1.6, 3.7-3.8, 10.4-10.9_

- [x] 3. 实现导出功能


  - 实现完整的导出逻辑，收集所有配置并生成JSON文件
  - _Requirements: 1.1-1.8, 5.1-5.7, 9.1-9.3_

- [x] 3.1 创建ExportCourseDataUseCase


  - 创建 `domain/usecase/ExportCourseDataUseCase.kt`
  - 实现 `collectExportData()` 方法收集学期、课程、设置数据
  - 实现 `exportToUri()` 方法将数据写入用户选择的Uri
  - 实现 `generateSuggestedFileName()` 方法生成建议文件名
  - _Requirements: 1.1-1.8, 5.1-5.3, 9.1-9.3_


- [x] 3.2 实现数据收集逻辑

  - 从SemesterRepository获取学期信息
  - 从CourseRepository获取课程列表
  - 从PreferencesManager获取节次设置和显示设置
  - 计算当前周数
  - 转换为导出数据模型
  - _Requirements: 1.4, 1.5, 1.6, 1.7, 10.1-10.9_



- [ ] 3.3 实现JSON序列化和文件写入
  - 使用kotlinx.serialization将数据序列化为JSON
  - 使用ContentResolver写入用户选择的Uri
  - 添加适当的错误处理
  - _Requirements: 1.3, 5.6, 8.1-8.8, 9.1-9.3_

- [x] 4. 实现导入功能


  - 实现完整的导入逻辑，解析JSON并恢复所有配置
  - _Requirements: 2.1-2.11, 5.4-5.7, 9.4-9.7_


- [x] 4.1 创建ImportCourseDataUseCase

  - 创建 `domain/usecase/ImportCourseDataUseCase.kt`
  - 实现 `parseFromUri()` 方法从Uri读取并解析JSON
  - 实现 `validateImportData()` 方法验证数据格式
  - 实现 `detectConflicts()` 方法检测冲突
  - 实现 `executeImport()` 方法执行导入
  - _Requirements: 2.1-2.11, 5.4-5.7, 7.1-7.7, 9.4-9.7_

- [x] 4.2 实现JSON解析和验证

  - 使用ContentResolver从Uri读取文件内容
  - 使用kotlinx.serialization解析JSON
  - 验证版本号和必需字段
  - 返回解析结果或错误信息
  - _Requirements: 2.2, 2.3, 8.7, 8.8, 9.4-9.7_

- [x] 4.3 实现冲突检测逻辑

  - 检查学期名称是否冲突
  - 检查节次设置是否与当前设置不同
  - 生成冲突信息供UI显示
  - _Requirements: 7.1-7.7_

- [x] 4.4 实现导入执行逻辑

  - 创建新学期（处理名称冲突）
  - 应用节次设置（如果用户选择）
  - 应用显示设置（如果用户选择）
  - 批量插入课程数据
  - 使用数据库事务确保原子性
  - _Requirements: 2.10, 2.11, 3.1-3.10, 9.5-9.7_

- [x] 5. 创建导出UI组件


  - 创建导出对话框和相关UI组件
  - _Requirements: 1.1, 1.2, 6.1-6.5_

- [x] 5.1 创建ExportDialog组件


  - 创建 `ui/components/ExportDialog.kt`
  - 显示导出选项（当前学期/所有学期）
  - 显示学期选择列表
  - 提供确认和取消按钮
  - _Requirements: 1.1, 1.2, 6.1-6.5_



- [ ] 5.2 集成文件选择器
  - 在SettingsScreen中注册ActivityResultContracts.CreateDocument
  - 设置MIME类型为application/json
  - 设置建议的文件名
  - 处理用户选择的Uri
  - _Requirements: 5.1-5.3, 5.7_

- [x] 6. 创建导入UI组件


  - 创建导入预览界面和相关UI组件
  - _Requirements: 2.1-2.11, 4.1-4.10, 7.1-7.7_

- [x] 6.1 创建ImportPreviewScreen


  - 创建 `ui/screens/course/ImportPreviewScreen.kt`（全新版本）
  - 显示学期信息（名称、日期范围、当前周数）
  - 显示节次设置摘要
  - 显示课程列表
  - 提供学期名称编辑功能
  - 提供节次设置应用选项
  - 提供显示设置应用选项
  - 提供确认和取消按钮
  - _Requirements: 4.1-4.10_

- [x] 6.2 创建ImportPreviewViewModel


  - 创建 `ui/screens/course/ImportPreviewViewModel.kt`（全新版本）
  - 管理导入数据状态
  - 管理导入选项状态
  - 处理导入执行
  - 处理错误和进度状态
  - _Requirements: 2.1-2.11, 9.4-9.7_

- [x] 6.3 创建冲突处理UI

  - 在ImportPreviewScreen中显示冲突信息
  - 提供冲突解决选项（创建新学期/重命名/取消）
  - 显示当前设置和导入设置的对比
  - _Requirements: 7.1-7.7_

- [x] 6.4 集成文件选择器

  - 在SettingsScreen中注册ActivityResultContracts.OpenDocument
  - 设置接受的MIME类型
  - 处理用户选择的Uri并导航到预览界面
  - _Requirements: 5.4-5.7_

- [x] 7. 更新SettingsViewModel


  - 重构SettingsViewModel中的导入导出相关代码
  - _Requirements: 1.1-1.8, 2.1-2.11, 9.1-9.7_

- [x] 7.1 删除旧的导入导出方法


  - 删除所有与ICS导入导出相关的方法
  - 删除旧的状态管理代码
  - _Requirements: 所有需求_

- [x] 7.2 添加新的导出方法

  - 添加 `prepareExport()` 方法显示导出对话框
  - 添加 `executeExportToUri()` 方法执行导出
  - 添加 `getSuggestedFileName()` 方法获取建议文件名
  - 管理导出状态和进度
  - _Requirements: 1.1-1.8, 9.1-9.3_

- [x] 7.3 添加新的导入方法

  - 添加 `startImport()` 方法启动导入流程
  - 管理导入状态
  - 处理导航到预览界面
  - _Requirements: 2.1-2.11, 9.4-9.7_

- [x] 8. 更新SettingsScreen


  - 更新设置界面中的导入导出按钮和逻辑
  - _Requirements: 1.1, 2.1, 5.1-5.7_

- [x] 8.1 更新导出按钮


  - 修改导出按钮的点击事件
  - 调用新的导出方法
  - 显示新的导出对话框
  - _Requirements: 1.1, 5.1-5.3_



- [ ] 8.2 更新导入按钮
  - 修改导入按钮的点击事件
  - 调用新的导入方法
  - 启动文件选择器

  - _Requirements: 2.1, 5.4-5.7_


- [ ] 8.3 添加进度提示
  - 显示导出/导入进度指示器
  - 显示成功/失败提示消息
  - _Requirements: 9.1-9.7_

- [x] 9. 实现数据转换扩展函数


  - 创建Domain Model和Export Data之间的转换函数
  - _Requirements: 3.1-3.10, 10.1-10.9_

- [x] 9.1 创建导出转换扩展


  - 创建 `domain/model/courseexport/ExportExtensions.kt`
  - 实现 `Semester.toSemesterInfo()`
  - 实现 `CourseSettings.toPeriodSettingsData()`
  - 实现 `CourseSettings.toDisplaySettingsData()`
  - 实现 `Course.toCourseData()`
  - _Requirements: 1.4-1.7, 10.1-10.9_


- [x] 9.2 创建导入转换扩展

  - 在同一文件中实现反向转换
  - 实现 `SemesterInfo.toSemester()`
  - 实现 `PeriodSettingsData.applyCourseSettings()`
  - 实现 `DisplaySettingsData.applyCourseSettings()`
  - 实现 `CourseData.toCourse(semesterId)`
  - _Requirements: 2.10, 3.1-3.10_

- [x] 10. 添加依赖注入配置


  - 在Hilt模块中提供新的Use Case
  - _Requirements: 所有需求_

- [x] 10.1 更新RepositoryModule


  - 在 `di/RepositoryModule.kt` 中添加 `ExportCourseDataUseCase` 的提供方法
  - 添加 `ImportCourseDataUseCase` 的提供方法
  - 注入所需的依赖
  - _Requirements: 所有需求_


- [x] 11. 更新导航配置

  - 添加导入预览界面的导航路由
  - _Requirements: 2.1-2.11_


- [x] 11.1 更新SaisonNavHost

  - 在 `ui/navigation/SaisonNavHost.kt` 中添加导入预览界面的路由
  - 配置导航参数（Uri）
  - 配置返回导航
  - _Requirements: 2.1-2.11_

- [x] 12. 添加字符串资源



  - 添加所有UI文本的字符串资源
  - _Requirements: 所有需求_

- [x] 12.1 添加中文字符串


  - 在 `values-zh-rCN/strings.xml` 中添加导入导出相关字符串

  - 包括按钮文本、提示消息、错误消息等
  - _Requirements: 所有需求_


- [x] 12.2 添加英文字符串

  - 在 `values/strings.xml` 中添加对应的英文字符串
  - _Requirements: 所有需求_

- [ ]* 13. 编写单元测试
  - 为核心逻辑编写单元测试
  - _Requirements: 所有需求_

- [ ]* 13.1 测试ExportCourseDataUseCase
  - 测试数据收集逻辑
  - 测试JSON序列化
  - 测试文件名生成
  - _Requirements: 1.1-1.8_

- [ ]* 13.2 测试ImportCourseDataUseCase
  - 测试JSON解析
  - 测试数据验证
  - 测试冲突检测
  - 测试导入逻辑
  - _Requirements: 2.1-2.11_

- [ ]* 13.3 测试数据模型序列化
  - 测试所有数据模型的序列化和反序列化
  - 测试边界情况
  - _Requirements: 8.1-8.8_

- [ ]* 14. 编写集成测试
  - 测试完整的导入导出流程
  - _Requirements: 所有需求_

- [ ]* 14.1 测试导出流程
  - 测试完整导出流程
  - 验证导出的JSON格式
  - _Requirements: 1.1-1.8_

- [ ]* 14.2 测试导入流程
  - 测试完整导入流程
  - 验证导入后的数据一致性
  - _Requirements: 2.1-2.11, 3.1-3.10_

- [ ]* 14.3 测试往返一致性
  - 导出后立即导入
  - 验证数据完全一致
  - _Requirements: 3.1-3.10, 10.1-10.9_
