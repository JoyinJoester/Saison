# 📅 Saison 任务管理应用



<div align="center">

![Saison](https://img.shields.io/badge/Saison-Task%20Manager-blue?style=for-the-badge)
[![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](LICENSE)

**一款优雅、功能强大的 Android 任务管理应用**

[功能特性](#-功能特性) • [安装说明](#-安装说明) • [使用指南](#-使用指南) • [技术栈](#️-技术栈) • [贡献指南](#-贡献指南)

</div>

---

## ✨ 功能特性

### 📋 核心功能
- **任务管理** - 完整的任务列表、详情、子任务管理
  - ✅ 自然语言快速添加
  - ✅ 优先级选择（4级）
  - ✅ 子任务支持
  - ✅ 任务搜索和筛选
- **日历视图** - 月/周/日/议程四种视图
  - ✅ 支持农历显示
  - ✅ 节假日标注
  - ✅ 事件拖拽调整
- **课程表** - 灵活的课程管理系统
  - ✅ A/B 周、单双周模式
  - ✅ 学期管理
  - ✅ ICS 文件导入
  - ✅ 课程分组
- **番茄钟** - 专注时间管理
  - ✅ 圆形进度条计时器
  - ✅ 工作/休息自动切换
  - ✅ 长休息逻辑
  - ✅ 今日统计
- **订阅管理** - 订阅服务跟踪
  - ✅ 自动续费提醒
  - ✅ 费用统计
  - ✅ 手动续费记录

### 🎨 界面设计
- **Material Design 3 Extended** - 现代化的 UI 设计
- **13 个季节主题** - 樱花、薄荷、琥珀、雪等精美主题
- **深色模式** - 支持浅色/深色/跟随系统
- **多语言支持** - 简体中文、English、日本語、Tiếng Việt
- **响应式布局** - 适配各种屏幕尺寸

### 🔒 数据安全
- **AES-256-GCM 加密** - 本地数据加密存储
- **WebDAV 同步** - 支持自建服务器同步
- **数据导入导出** - 课程表 ICS 格式支持
- **隐私保护** - 数据完全本地存储

### 📱 实用功能
- **桌面小部件** - 任务和课程表小部件
- **通知提醒** - 任务和事件提醒
- **节拍器** - 音乐练习辅助工具
- **日常习惯** - 习惯养成跟踪
- **事件管理** - 重要事件倒计时

---

## 📥 安装说明

### 系统要求
- Android 10.0 (API 29) 及以上版本
- 约 30MB 存储空间

### 下载安装

#### 方式一: 从 Release 下载
1. 前往 [Releases](https://github.com/JoyinJoester/saison/releases) 页面
2. 下载最新版本的 APK 文件
3. 在 Android 设备上安装 APK

#### 方式二: 从源码编译
```bash
# 克隆仓库
git clone https://github.com/JoyinJoester/saison.git
cd saison

# 使用 Gradle 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

---

## 📖 使用指南

### 首次使用

1. **选择主题**
   - 首次打开应用，选择您喜欢的季节主题
   - 可在设置中随时更改

2. **设置语言**
   - 支持简体中文、English、日本語、Tiếng Việt
   - 自动跟随系统语言

### 任务管理

#### 添加任务
1. 点击主页的 ➕ 按钮
2. 使用自然语言输入，如"明天下午3点开会"
3. 或手动填写：
   - 任务标题
   - 截止日期和时间
   - 优先级（低/中/高/紧急）
   - 子任务（可选）
   - 备注（可选）
4. 点击保存

#### 管理任务
- 点击任务查看详情
- 滑动完成或删除任务
- 使用筛选器查看不同状态的任务
- 搜索功能快速定位

### 日历功能

#### 查看日历
1. 切换到日历标签页
2. 支持四种视图：
   - **月视图**: 7x6 网格，显示农历和节假日
   - **周视图**: 时间轴显示，事件卡片
   - **日视图**: 24小时详细视图
   - **议程视图**: 列表形式，按日期分组

#### 添加事件
1. 在日历视图中点击日期
2. 填写事件信息
3. 设置提醒时间
4. 保存事件

### 课程表管理

#### 创建学期
1. 进入课程表页面
2. 点击学期设置
3. 设置学期开始日期和周数
4. 配置上课时间段

#### 添加课程
1. 点击 ➕ 添加课程
2. 填写课程信息：
   - 课程名称
   - 教师姓名
   - 上课地点
   - 星期和节次
   - 周次（全部/A周/B周/单周/双周）
3. 保存课程

#### 导入课程表
1. 准备 ICS 格式的课程表文件
2. 点击导入按钮
3. 选择文件并预览
4. 确认导入

### 番茄钟使用

1. 切换到番茄钟标签页
2. 点击开始按钮
3. 专注工作 25 分钟
4. 休息 5 分钟
5. 每 4 个番茄钟后长休息 15 分钟

#### 自定义设置
- 工作时长（默认 25 分钟）
- 短休息时长（默认 5 分钟）
- 长休息时长（默认 15 分钟）
- 长休息间隔（默认 4 个番茄钟）

### 订阅管理

1. 切换到订阅标签页
2. 添加订阅服务：
   - 服务名称
   - 费用
   - 续费周期
   - 下次续费日期
3. 查看统计信息
4. 接收续费提醒

### 数据同步

#### 配置 WebDAV
1. 进入设置 → WebDAV 备份
2. 填写服务器信息：
   - 服务器地址
   - 用户名
   - 密码
3. 测试连接
4. 启用自动备份

#### 手动备份
1. 进入设置 → WebDAV 备份
2. 点击"立即备份"
3. 等待备份完成

#### 恢复数据
1. 进入设置 → WebDAV 备份
2. 查看备份列表
3. 选择要恢复的备份
4. 确认恢复

---

## 🛠️ 技术栈

### 核心技术
- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化 UI 框架
- **Material Design 3 Extended** - UI 设计规范

### 架构组件
- **MVVM + Clean Architecture** - 清晰的架构分层
- **Hilt/Dagger** - 依赖注入
- **Room Database** - 本地数据持久化
- **DataStore** - 偏好设置存储
- **Kotlin Coroutines** - 异步编程
- **Kotlin Flow** - 响应式数据流
- **Navigation Compose** - 导航管理

### 安全组件
- **AES-256-GCM** - 数据加密标准
- **EncryptionManager** - 加密管理器

### 第三方库
- **OkHttp** - 网络请求
- **Sardine** - WebDAV 客户端
- **ICS Parser** - 课程表导入

### 构建工具
- **Gradle 8.7** - 构建系统
- **Android Gradle Plugin 8.5.0**
- **Kotlin 1.9.0**

---

## 🎨 主题

Saison 提供 13 个精心设计的季节主题：

| 主题 | 描述 | 主题 | 描述 |
|------|------|------|------|
| 🌸 樱花 (Sakura) | 温柔粉色 | 🌿 薄荷 (Mint) | 清新绿色 |
| 🍂 琥珀 (Amber) | 温暖橙色 | ❄️ 雪 (Snow) | 纯净白色 |
| 🌧️ 雨 (Rain) | 宁静蓝色 | 🍁 枫叶 (Maple) | 秋日红色 |
| 🌊 海洋 (Ocean) | 深邃蓝色 | 🌅 日落 (Sunset) | 渐变橙红 |
| 🌲 森林 (Forest) | 自然绿色 | 💜 薰衣草 (Lavender) | 优雅紫色 |
| 🏜️ 沙漠 (Desert) | 沙漠金色 | 🌌 极光 (Aurora) | 梦幻渐变 |
| 🎨 动态 (Dynamic) | Android 12+ 动态取色 | | |

---

## 🌍 多语言支持

- 🇨🇳 简体中文
- 🇺🇸 English
- 🇯🇵 日本語
- 🇻🇳 Tiếng Việt

---

## 📁 项目结构

```
app/src/main/java/takagi/ru/saison/
├── data/                    # 数据层
│   ├── local/              # 本地数据
│   │   ├── database/       # Room 数据库
│   │   ├── datastore/      # DataStore
│   │   └── encryption/     # 加密管理
│   ├── remote/             # 远程数据
│   │   ├── calendar/       # 日历提供者
│   │   └── webdav/         # WebDAV 客户端
│   └── repository/         # Repository 层
├── domain/                  # 领域层
│   ├── model/              # 数据模型
│   ├── mapper/             # 映射器
│   └── usecase/            # 用例
├── ui/                      # UI 层
│   ├── screens/            # 屏幕
│   │   ├── calendar/       # 日历
│   │   ├── task/           # 任务
│   │   ├── course/         # 课程表
│   │   ├── pomodoro/       # 番茄钟
│   │   └── settings/       # 设置
│   ├── components/         # 组件
│   ├── navigation/         # 导航
│   ├── theme/              # 主题
│   └── widget/             # 桌面小部件
├── notification/           # 通知系统
└── util/                    # 工具类
```

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 如何贡献

1. **Fork 本仓库**
2. **创建特性分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送到分支** (`git push origin feature/AmazingFeature`)
5. **创建 Pull Request**

### 报告问题

如果您发现 Bug 或有功能建议：
1. 前往 [Issues](https://github.com/JoyinJoester/saison/issues) 页面
2. 搜索是否已有相关问题
3. 创建新 Issue 并详细描述

### 代码规范
- 遵循 Kotlin 官方代码风格
- 添加必要的注释
- 编写单元测试
- 保持代码简洁清晰

---

## 📄 许可证

本项目采用 GPL-3.0 许可证 - 详见 [LICENSE](LICENSE) 文件

```
GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2025 Saison Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```

---

## 👨‍💻 作者

**Saison Team**
- GitHub: [@JoyinJoester](https://github.com/JoyinJoester)

---

## 🙏 致谢

感谢所有为 Saison 做出贡献的开发者！

特别感谢：
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化 UI 框架
- [Material Design 3](https://m3.material.io/) - 设计规范
- [Room](https://developer.android.com/training/data-storage/room) - 数据库框架
- [Hilt](https://dagger.dev/hilt/) - 依赖注入框架

---

## 📞 支持

如果您觉得这个项目有帮助，请给一个 ⭐️ Star！

有问题或建议？
- 💬 Issues: [GitHub Issues](https://github.com/JoyinJoester/saison/issues)
- 📧 Email: Joyin8888@foxmail.com

---

## 📊 开发进度

![Progress](https://img.shields.io/badge/进度-58%25-green)
![Tasks](https://img.shields.io/badge/任务-15/26-blue)
![Quality](https://img.shields.io/badge/质量-优秀-brightgreen)
![Status](https://img.shields.io/badge/状态-开发中-yellow)

### 已完成功能
- ✅ 任务管理系统
- ✅ 日历视图（月/周/日/议程）
- ✅ 课程表管理
- ✅ 番茄钟计时器
- ✅ 订阅管理
- ✅ 13 个季节主题
- ✅ 多语言支持
- ✅ WebDAV 同步
- ✅ 数据加密

### 开发中功能
- 🚧 通知系统
- 🚧 桌面小部件
- 🚧 节拍器
- 🚧 性能优化

---

<div align="center">

[回到顶部](#-saison-任务管理应用)

</div>
