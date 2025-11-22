# Requirements Document

## Introduction

完整的课程表导入导出系统，确保用户可以导出所有课程配置（包括节次设置、当前周数、学期信息等），并让另一个用户导入后看到完全一致的课程表界面和设置。

## Glossary

- **System**: Saison课程表导入导出系统
- **Export Package**: 包含所有课程表配置的完整数据包
- **Period Settings**: 节次设置，包括总节次数、每节课时长、课间休息时长、第一节课开始时间
- **Semester Config**: 学期配置，包括学期名称、开始日期、结束日期、当前周数
- **Course Data**: 课程数据，包括课程名称、教师、地点、时间、周数模式等
- **Display Settings**: 显示设置，包括是否显示周末、时间显示格式等
- **SAF**: Storage Access Framework，Android存储访问框架

## Requirements

### Requirement 1

**User Story:** 作为用户，我想导出我的完整课程表配置，以便分享给其他人或备份

#### Acceptance Criteria

1. WHEN THE User 点击设置中的"导出课程表"按钮, THE System SHALL 显示导出选项对话框
2. WHEN THE User 选择导出选项并确认, THE System SHALL 打开系统文件选择器让用户选择保存位置
3. WHEN THE User 选择保存位置, THE System SHALL 生成包含所有配置的JSON文件
4. THE System SHALL 在JSON文件中包含学期信息（名称、开始日期、结束日期、当前周数）
5. THE System SHALL 在JSON文件中包含节次设置（总节次、时长、休息时长、开始时间）
6. THE System SHALL 在JSON文件中包含所有课程数据（名称、教师、地点、时间、周数模式、颜色）
7. THE System SHALL 在JSON文件中包含显示设置（是否显示周末、时间格式等）
8. WHEN 导出成功, THE System SHALL 显示成功提示消息

### Requirement 2

**User Story:** 作为用户，我想导入他人分享的课程表配置，以便快速设置我的课程表

#### Acceptance Criteria

1. WHEN THE User 点击设置中的"导入课程表"按钮, THE System SHALL 打开系统文件选择器让用户选择JSON文件
2. WHEN THE User 选择JSON文件, THE System SHALL 解析文件内容并验证格式
3. IF 文件格式无效, THEN THE System SHALL 显示错误提示并终止导入
4. WHEN 文件格式有效, THE System SHALL 显示导入预览界面
5. THE System SHALL 在预览界面显示将要导入的学期信息
6. THE System SHALL 在预览界面显示将要导入的节次设置
7. THE System SHALL 在预览界面显示将要导入的课程列表
8. THE System SHALL 允许用户选择是否应用节次设置
9. THE System SHALL 允许用户选择是否应用显示设置
10. WHEN THE User 确认导入, THE System SHALL 创建新学期并导入所有数据
11. WHEN 导入成功, THE System SHALL 显示成功提示并跳转到新创建的学期

### Requirement 3

**User Story:** 作为用户，我想确保导入的课程表与原始课程表完全一致，包括所有时间和显示设置

#### Acceptance Criteria

1. WHEN THE System 导入节次设置, THE System SHALL 应用相同的总节次数
2. WHEN THE System 导入节次设置, THE System SHALL 应用相同的每节课时长
3. WHEN THE System 导入节次设置, THE System SHALL 应用相同的课间休息时长
4. WHEN THE System 导入节次设置, THE System SHALL 应用相同的第一节课开始时间
5. WHEN THE System 导入学期配置, THE System SHALL 创建具有相同开始和结束日期的学期
6. WHEN THE System 导入学期配置, THE System SHALL 设置相同的当前周数
7. WHEN THE System 导入课程数据, THE System SHALL 保留所有课程的时间、地点、教师信息
8. WHEN THE System 导入课程数据, THE System SHALL 保留所有课程的周数模式
9. WHEN THE System 导入显示设置, THE System SHALL 应用相同的周末显示设置
10. WHEN THE System 导入显示设置, THE System SHALL 应用相同的时间显示格式

### Requirement 4

**User Story:** 作为用户，我想在导入前预览将要导入的内容，以便确认是否正确

#### Acceptance Criteria

1. THE System SHALL 在导入预览界面显示学期名称和日期范围
2. THE System SHALL 在导入预览界面显示当前周数
3. THE System SHALL 在导入预览界面显示节次设置摘要
4. THE System SHALL 在导入预览界面显示课程总数
5. THE System SHALL 在导入预览界面显示课程列表（课程名、时间、地点）
6. THE System SHALL 允许用户在预览界面修改学期名称
7. THE System SHALL 允许用户在预览界面选择是否应用节次设置
8. THE System SHALL 允许用户在预览界面选择是否应用显示设置
9. THE System SHALL 提供取消按钮让用户放弃导入
10. THE System SHALL 提供确认按钮让用户执行导入

### Requirement 5

**User Story:** 作为用户，我想使用标准的Android文件选择器，以便选择任意位置保存或读取文件

#### Acceptance Criteria

1. WHEN 导出时, THE System SHALL 使用ActivityResultContracts.CreateDocument启动文件选择器
2. WHEN 导出时, THE System SHALL 建议默认文件名包含学期名称和日期
3. WHEN 导出时, THE System SHALL 设置文件类型为application/json
4. WHEN 导入时, THE System SHALL 使用ActivityResultContracts.OpenDocument启动文件选择器
5. WHEN 导入时, THE System SHALL 接受application/json和所有文件类型
6. THE System SHALL 使用ContentResolver读写用户选择的Uri
7. THE System SHALL 正确处理用户取消文件选择的情况

### Requirement 6

**User Story:** 作为用户，我想导出多个学期的课程表，以便一次性备份所有数据

#### Acceptance Criteria

1. THE System SHALL 在导出选项中提供"当前学期"和"所有学期"选项
2. WHEN THE User 选择"所有学期", THE System SHALL 导出所有学期的完整配置
3. WHEN 导出多个学期, THE System SHALL 在JSON中使用数组结构存储多个学期
4. WHEN 导入多学期文件, THE System SHALL 显示所有学期的列表供用户选择
5. THE System SHALL 允许用户选择导入其中一个或多个学期

### Requirement 7

**User Story:** 作为用户，我想在导入时处理冲突，以便避免覆盖现有数据

#### Acceptance Criteria

1. WHEN 导入的学期名称与现有学期冲突, THE System SHALL 提示用户选择处理方式
2. THE System SHALL 提供"创建新学期"选项
3. THE System SHALL 提供"重命名导入的学期"选项
4. THE System SHALL 提供"取消导入"选项
5. WHEN THE User 选择重命名, THE System SHALL 在学期名称后添加"(导入)"后缀
6. WHEN 节次设置与当前设置不同, THE System SHALL 提示用户是否应用新设置
7. THE System SHALL 显示当前设置和导入设置的对比

### Requirement 8

**User Story:** 作为开发者，我想使用JSON格式存储数据，以便易于调试和扩展

#### Acceptance Criteria

1. THE System SHALL 使用JSON格式存储导出数据
2. THE System SHALL 在JSON中包含版本号字段
3. THE System SHALL 在JSON中包含导出时间戳
4. THE System SHALL 在JSON中包含应用版本号
5. THE System SHALL 使用清晰的字段命名（英文）
6. THE System SHALL 使用合理的JSON结构（嵌套对象和数组）
7. THE System SHALL 在导入时验证JSON格式和必需字段
8. IF JSON版本不兼容, THEN THE System SHALL 显示错误提示

### Requirement 9

**User Story:** 作为用户，我想在导入导出过程中看到进度提示，以便了解操作状态

#### Acceptance Criteria

1. WHEN 导出开始, THE System SHALL 显示"正在导出..."提示
2. WHEN 导出成功, THE System SHALL 显示"导出成功"提示
3. WHEN 导出失败, THE System SHALL 显示具体错误信息
4. WHEN 导入开始, THE System SHALL 显示"正在解析..."提示
5. WHEN 导入成功, THE System SHALL 显示"导入成功，已创建X门课程"提示
6. WHEN 导入失败, THE System SHALL 显示具体错误信息
7. THE System SHALL 在长时间操作时显示加载指示器

### Requirement 10

**User Story:** 作为用户，我想导出的文件包含所有必要信息，以便在新设备上完整恢复课程表

#### Acceptance Criteria

1. THE System SHALL 在导出文件中包含学期的开始日期和结束日期
2. THE System SHALL 在导出文件中包含当前是第几周
3. THE System SHALL 在导出文件中包含每节课的具体开始和结束时间
4. THE System SHALL 在导出文件中包含课程的周数模式（如单周、双周、全周）
5. THE System SHALL 在导出文件中包含课程的星期几
6. THE System SHALL 在导出文件中包含课程的节次范围
7. THE System SHALL 在导出文件中包含课程的颜色信息
8. THE System SHALL 在导出文件中包含课程的备注信息
9. THE System SHALL 确保导入后课程表显示与导出时完全一致
