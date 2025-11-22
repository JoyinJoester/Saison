# 导出功能改进 - 用户自选保存位置

## 概述

修改了课程表导出功能，现在用户可以在导出时自由选择文件保存位置，而不是固定保存到应用私有目录。

## 技术实现

### 使用 Storage Access Framework (SAF)

采用 Android 的 Storage Access Framework，通过 `ActivityResultContracts.CreateDocument` 让用户选择保存位置。

### 修改的文件

1. **IcsGenerator.kt**
   - 添加 `writeToUri()` 方法，支持写入到用户选择的 Uri
   - 保留旧的 `writeToFile()` 方法并标记为 `@Deprecated`

2. **EnhancedIcsGenerator.kt**
   - 添加 `writeToUri()` 方法，调用基础生成器的新方法
   - 保留旧方法并标记为废弃

3. **EnhancedIcsExportUseCase.kt**
   - 添加 `exportWithFullConfigToUri()` 方法，接受用户选择的 Uri
   - 添加 `getSuggestedFileName()` 方法，生成建议的文件名供 UI 使用
   - 保留旧方法并标记为废弃

4. **CourseViewModel.kt**
   - 添加 `prepareExport()` 方法，准备导出并生成建议的文件名
   - 添加 `exportCurrentSemesterToUri()` 方法，执行导出到用户选择的位置
   - 添加 `suggestedFileName` 状态流
   - 保留旧方法并标记为废弃

5. **CourseScreen.kt**
   - 添加 `exportLauncher`，使用 `CreateDocument` 合约启动文件选择器
   - 修改导出流程：点击导出 → 生成文件名 → 启动 SAF → 用户选择位置 → 执行导出
   - 添加导出成功提示对话框

## 用户体验流程

1. 用户点击"导出"按钮
2. 系统自动生成建议的文件名（格式：`课程表_[学期名称]_完整_[日期].ics`）
3. 打开系统文件选择器，用户可以：
   - 选择保存位置（下载文件夹、云盘、其他应用等）
   - 修改文件名
   - 取消操作
4. 用户确认后，文件保存到选择的位置
5. 显示导出成功对话框，提供分享选项

## 优势

- **用户自主性**：用户可以选择任意位置保存文件
- **云盘集成**：可以直接保存到 Google Drive、OneDrive 等云存储
- **文件管理**：更容易找到和管理导出的文件
- **系统标准**：符合 Android 现代文件访问规范
- **向后兼容**：保留旧方法，不影响现有代码

## 文件名格式

建议的文件名格式：
- 完整配置：`课程表_[学期名称]_完整_[日期].ics`
- 兼容模式：`课程表_[学期名称]_[日期].ics`

示例：
- `课程表_2024春季学期_完整_20241122.ics`
- `课程表_2024春季学期_20241122.ics`

## 注意事项

1. 需要用户授予存储权限（SAF 会自动处理）
2. 旧的导出方法已标记为废弃，建议逐步迁移
3. 导出成功后会显示分享对话框，方便用户分享文件

## 修复记录

### 编译错误修复（2024-11-22）

1. **缺少导入**：在 `CourseScreen.kt` 中添加了 `ExportSuccessDialog` 的导入
2. **Context 传递**：由于 `CourseViewModel` 继承自普通 `ViewModel` 而非 `AndroidViewModel`，修改了 `exportCurrentSemesterToUri()` 方法，从 UI 层传入 `Context` 参数，而不是在 ViewModel 中调用 `getApplication()`
