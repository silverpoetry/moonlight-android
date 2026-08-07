<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_app.png" width="128" alt="Moonlight icon">
  <h1>Moonlight for Android</h1>
  <p>面向远程桌面、原生触控板输入和跨设备协作的 Moonlight 独立分支</p>

  [![Release](https://img.shields.io/github/v/release/silverpoetry/moonlight-android?style=flat-square)](https://github.com/silverpoetry/moonlight-android/releases)
  [![License](https://img.shields.io/github/license/silverpoetry/moonlight-android?style=flat-square)](LICENSE.txt)
  [![Android](https://img.shields.io/badge/Android-6.0%2B-3DDC84?logo=android&logoColor=white&style=flat-square)](https://developer.android.com/)
</div>

[特色功能](#特色功能) · [安装与迁移](#安装与迁移) · [构建与质量门禁](#构建与质量门禁) · [架构文档](ARCHITECTURE.md) · [变更记录](CHANGELOG.md)

Moonlight for Android 是一个独立维护的 Moonlight Android 分支。项目保留
GameStream/Sunshine 串流链路，并围绕 Android 手机、平板和大屏设备重新整理界面、输入、
文件协作和运行时架构。当前产品名称为 **Moonlight**，普通 Release 包名为
`com.silverpoetry.moonlight`。

## 特色功能

### 现代 Android 界面

- 使用 Jetpack Compose 和 Material 3 设计语言，统一颜色、字体、形状、深浅色主题和
  Android 12+ 动态取色。
- 主界面、主机应用列表、系统设置、串流快捷菜单、关于和文件操作共享同一套组件和排版
  规则。
- 设置按任务组织为视频与显示、音频、输入、虚拟控制、剪贴板与文件、串流界面、应用
  外观、系统与无障碍、备份与恢复等区域；短选项使用选择器，连续值使用滑块，复杂
  编辑使用独立页面或对话框。
- 串流返回菜单支持状态切换和一次性动作两种语义。麦克风、性能面板等状态控制会保留
  菜单；截屏、快捷键和键盘等一次性动作完成后自动收起。

### 输入与触控板

- 触控板模式发送完整的多指帧，保留 pointer ID、归一化坐标、按下/移动/抬起和取消
  生命周期，主机端按原生触控板语义处理。
- 普通鼠标、绝对定位和触控板输入分别经过明确的控制器路径；本地光标预测和远端发送
  使用同一份规范化位置，避免两套算法产生漂移。
- 支持双指右键、双指滚动/缩放、多指手势、三指轻点呼出键盘、单双指长按和按住拖动。
  三指键盘候选具有时间域仲裁，普通两指操作、长按和重按可以取消候选。
- 支持气压计融合重按。压力阈值和最短触摸持续时间可配置，手指离开屏幕才结束重按；
  没有气压计或未启用该模式时使用触控板长按。
- 虚拟手柄和虚拟按键布局分别保存横屏、竖屏文档，支持自定义布局、震动、运动传感器、
  USB/蓝牙手柄以及 NVIDIA SHIELD 扩展。

### 串流与窗口体验

- 串流会话采用显式的 `create -> start -> active -> stop -> destroy` 生命周期，连接、
  Surface、解码器、音频、方向和输入捕获各有明确的资源所有者。
- Compose 页面负责启动前的界面和状态，视频 Surface、解码器、实时输入和音频仍由低延迟
  原生链路管理。连接准备完成后再切换到实际串流界面，避免用静态页面伪装加载状态。
- 支持横竖屏、自由窗口、分屏、画中画、刘海/挖孔区域、外接显示器、HDR、帧率提示和
  可选的视频隐藏。方向按钮只改变当前会话的方向覆盖，不与系统自动旋转策略反复争抢。
- 设置变更按即时生效、下一次会话生效和重新创建资源三类明确处理，运行态不直接读取
  SharedPreferences。

### 剪贴板、文件与麦克风

- 一个剪贴板同步开关统一管理文本、图片、文件和文件夹。
- 文件复制时只交换轻量清单或占位信息；只有另一端明确粘贴、拉取或分享时才读取目录
  内容并启动传输，大型目录不会在复制动作发生时占用输入通道。
- 串流菜单可拉取主机剪贴板中的文件/文件夹；Android 系统分享菜单可以把本地文件推送
  到主机桌面。传输目标使用用户选择的目录或桌面语义，文件冲突和取消由传输控制器处理。
- 麦克风复用协商后的音频 UDP 通道，支持串流中启停、静音、单声道/双声道采集和格式
  转换，不额外增加固定监听端口。

### 配置迁移

- “备份与恢复”只保留“导出配置”和“导入配置”两个入口。
- 导出使用 Android 标准“另存为”界面，用户选择具体 ZIP 文件名和保存位置；应用不申请
  整个目录的长期授权，也不经过分享面板。
- 配置 ZIP v2 包含 App 设置、主机连接信息和客户端身份三类组件。导入时可分别选择：
  - App 设置；
  - 主机连接信息（主机名称、地址、端口、MAC 和固定的主机证书）；
  - 客户端身份（匹配的客户端证书和私钥，作为不可拆分的原子项）。
- 只导入主机连接信息会保留连接资料并使用当前设备身份重新确认配对状态；导入客户端
  身份则用于迁移已有配对授权。导入前完成 ZIP 路径、大小、摘要、类型和证书密钥匹配
  校验，写入过程按组件回滚。

## 项目架构

项目采用“平台无关核心 + Android 适配层 + 原生协议层”的分层结构。Compose 只负责
非实时展示，输入、音频、视频和协议回调使用有界、可取消、可验证的实时路径。

```text
Android entry points and Compose UI
             │
             ▼
feature controllers and immutable UI state
             │
             ▼
core:hosts   core:settings   core:transfer   core:virtual-controls
             │
             ▼
Android/protocol adapters and native stream pipeline
             │
             ▼
moonlight-common-c / JNI  <──>  Sunshine
```

| 模块 | 职责 |
| --- | --- |
| `app` | Android Activity、Compose UI、生命周期组合、SAF/URI、数据库、mDNS、NvHTTP、JNI、串流和发布打包 |
| `core:hosts` | 不可变主机模型、发现/配对/可达性契约、主机操作用例 |
| `core:settings` | 类型化设置键、默认值、校验、迁移、快照和运行态设置模型 |
| `core:transfer` | 文件清单协议、校验、编码和传输生命周期 |
| `core:virtual-controls` | 虚拟手柄/按键布局值对象、文档编码和仓库契约 |
| `core:shield-controller-extensions` | NVIDIA SHIELD 外设兼容库 |
| `build-logic` | 依赖边界、质量门禁、发布安全策略和 SBOM 任务 |

关键约束：每个可变状态只有一个所有者；UI 通过不可变状态和语义事件工作；实时路径
不访问磁盘、不等待远端、不创建无界队列；文件清单、能力协商和真正的数据读取分开；
销毁后的回调不能写入新的会话或页面。完整设计依据见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 兼容性

- 最低系统：Android 6.0（API 23）。
- 编译和目标 API：37。
- 推荐产品：`nonRootRelease`，包名 `com.silverpoetry.moonlight`。
- Root 产品：`rootRelease`，包名 `com.silverpoetry.moonlight.root`，面向确实需要
  root 变体能力的设备；该变体有单独的最高系统版本限制。
- Android 设备需要与兼容的 Sunshine/Moonlight 主机端配套，触控板、麦克风和新版文件
  传输能力以双方协商结果为准。

Android 客户端、Moonlight Qt、Sunshine 和共享 `moonlight-common-c` 应尽量使用同一
发布批次。当前仓库固定的 common-c 修订为
[`ac7f234`](app/src/main/jni/moonlight-core/moonlight-common-c)。配套项目如下：

| 项目 | 职责 | 仓库 |
| --- | --- | --- |
| Moonlight Android | Android 客户端、移动端输入、剪贴板、文件、麦克风和 UI | 当前仓库 |
| Sunshine | Windows 主机、原生触控板注入、剪贴板和麦克风接收 | [silverpoetry/Sunshine](https://github.com/silverpoetry/Sunshine) |
| Moonlight Qt | Windows 客户端、桌面输入和对称文件剪贴板 | [silverpoetry/moonlight-qt](https://github.com/silverpoetry/moonlight-qt) |

## 安装与迁移

从 [GitHub Releases](https://github.com/silverpoetry/moonlight-android/releases) 下载
对应设备的 APK 和校验文件。普通设备安装 `nonRootRelease`；诊断问题时使用带有
`.debug` 后缀的 Debug 包，不把诊断日志带入正式包。

Release 使用完整 R8 代码优化、混淆和资源收缩。发布时应同时归档 APK、R8 mapping、
SHA-256、版本信息和签名证书摘要。普通 Release 可以覆盖安装同包名的旧版本并保留
设置、主机和客户端身份；包名不同的历史测试包需要先按迁移说明导出或迁移数据，再安装
正式包。

## 构建与质量门禁

需要 JDK 21、Android SDK、Android NDK `27.0.12077973` 和 Git 子模块：

```powershell
git submodule update --init --recursive
.\gradlew.bat nonRootRelease
```

日常开发只构建普通 non-root Release。提交前运行单变体门禁：

```powershell
.\gradlew.bat verifyNonRootRelease --no-daemon
```

该门禁包含核心模块测试、应用 Release 单元测试、Release Lint、架构与安全检查、依赖
和原生依赖校验以及 APK 构建。完整本地门禁为：

```powershell
.\gradlew.bat verifyLocal --no-daemon
```

设备验证、升级覆盖安装、回滚演练、跨客户端剪贴板/麦克风矩阵和发布证据要求见
[TESTING.md](TESTING.md) 与 [Release Runbook](docs/architecture/RELEASE_RUNBOOK.md)。

## 隐私与安全

- 剪贴板、文件和麦克风能力只在串流会话中、双方协商后启用。
- 文件复制不会在用户按下复制键时读取或上传整个目录；真实内容只在远端明确请求后
  进入传输队列。
- 主机数据库、固定主机证书、客户端证书和私钥保存在应用私有目录，并排除 Android
  自动备份。显式配置导出由用户主动发起，ZIP 内含私钥时应妥善保存。
- Release 日志不记录剪贴板正文、文件路径、证书、私钥、麦克风采样或逐事件实时轨迹。
- 依赖来源、许可证文本和 SBOM 规则见 [DEPENDENCIES.md](DEPENDENCIES.md) 和
  `app/src/main/assets/third_party_licenses`。

## 文档索引

- [架构总则](ARCHITECTURE.md)
- [变更记录](CHANGELOG.md)
- [参与贡献](CONTRIBUTING.md)
- [贡献者、代码来源与许可证边界](CONTRIBUTORS.md)
- [第三方声明](THIRD_PARTY_NOTICES.md)
- [测试与发布前验证](TESTING.md)
- [依赖和 SBOM 策略](DEPENDENCIES.md)
- [隐私说明](PRIVACY.md)
- [安全策略](SECURITY.md)
- [设置架构与配置迁移](docs/architecture/SETTINGS_ARCHITECTURE.md)
- [模块边界](docs/architecture/GRADLE_MODULE_BOUNDARIES.md)
- [实时线程约束](docs/architecture/REALTIME_THREADING.md)
- [串流会话生命周期](docs/architecture/STREAM_SESSION_LIFECYCLE.md)
- [输入行为基线](docs/architecture/INPUT_BEHAVIOR_BASELINE.md)
- [窗口、视口和坐标系统](docs/architecture/STREAM_VIEWPORT_GEOMETRY.md)
- [Android 安全边界](docs/architecture/ANDROID_SECURITY_BOUNDARIES.md)
- [重构路线图与完成记录](docs/architecture/REFACTORING_ROADMAP.md)
- [发布 Runbook](docs/architecture/RELEASE_RUNBOOK.md)
- [架构决策记录](docs/adr)
- [质量基线](QUALITY_BASELINE.md) · [质量改进计划](QUALITY_IMPROVEMENT_PLAN.md) · [迁移日志](QUALITY_MIGRATION_LOG.md)

## 贡献与许可

提交问题或补丁时，请说明设备、Android 版本、产品变体、主机端版本和可复现步骤；涉及
输入、传输、协议、权限或串流生命周期的变更应同时提供对应的契约测试或验证记录。
开发规则见 [CONTRIBUTING.md](CONTRIBUTING.md)。请勿上传个人设备地址、证书、私钥、
剪贴板内容、文件路径或麦克风录音。

本项目是 Moonlight Android 的独立分支，相关上游作者、历史来源、外部移植和许可证
记录见 [CONTRIBUTORS.md](CONTRIBUTORS.md) 和
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。项目整体许可为
[GPL-3.0](LICENSE.txt)，第三方依赖的许可证随源码和安装包提供。Moonlight、Sunshine
以及设备和厂商名称归各自权利人所有；项目与相关品牌不存在官方隶属或背书关系。
