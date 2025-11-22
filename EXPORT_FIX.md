# 导出功能修复

## 问题
点击导出按钮后，文件选择器没有弹出。

## 原因
之前的实现使用了 `showExportDialog` 状态和 `LaunchedEffect`，但是状态立即被设置回 `false`，导致文件选择器没有正确启动。

## 解决方案
直接在 `onExportClick` 回调中启动文件选择器，不再使用中间状态。

### 修改内容

**CourseScreen.kt:**
```kotlin
onExportClick = {
    // 直接启动文件选择器
    coroutineScope.launch {
        val suggestedFileName = viewModel.prepareExport(compatibilityMode = false)
        if (suggestedFileName != null) {
            exportLauncher.launch(suggestedFileName)
        }
    }
}
```

## 使用流程

1. 用户点击课程表页面顶部的导出按钮
2. 系统生成建议的文件名（例如：`课程表_2024春季学期_完整_20241122.ics`）
3. **立即弹出系统文件选择器**
4. 用户选择保存位置（下载文件夹、云盘等）
5. 文件保存成功后显示成功对话框

## 测试步骤

1. 打开应用，进入课程表页面
2. 点击顶部的导出按钮（文件上传图标）
3. 应该立即看到系统文件选择器弹出
4. 选择保存位置并确认
5. 等待导出完成，应该看到成功提示
