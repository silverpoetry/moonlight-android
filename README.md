<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_app.png" width="128" alt="Moonlight icon">
  <h1>Moonlight for Android</h1>
  <p>面向远程桌面、原生触控板输入和跨设备协作的 Moonlight Android 独立分支</p>

  [![Release](https://img.shields.io/github/v/release/silverpoetry/moonlight-android?style=flat-square)](https://github.com/silverpoetry/moonlight-android/releases)
  [![License](https://img.shields.io/github/license/silverpoetry/moonlight-android?style=flat-square)](LICENSE.txt)
  [![Android](https://img.shields.io/badge/Android-5.0%2B-3DDC84?logo=android&logoColor=white&style=flat-square)](https://developer.android.com/)
</div>

> [!IMPORTANT]
> 这是社区维护的独立分支，不是 Moonlight 官方发行版。完整项目传承为
> [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) →
> [Axixi 的 Android 分支](https://github.com/Axixi2233/moonlight-android) →
> 当前重构分支。上游、Axixi 分支和外部代码来源的作者信息均保留在
> [CONTRIBUTORS.md](CONTRIBUTORS.md) 与 Git 历史中。

## 项目定位

本项目继承 Moonlight Android 的串流基础和 Axixi 分支积累的移动端 UI、输入与手柄能力，并在此基础上进行独立的大规模重构。当前目标不是继续堆叠 Activity 内的功能分支，而是让主机、设置、输入、文件传输和串流生命周期各自拥有明确边界、单一状态所有者和可验证的契约。

需要扩展协议的功能应与以下配套项目使用同一发布版本：

| 组件 | 职责 | 仓库 |
| --- | --- | --- |
| Moonlight Android | Android 客户端、触控输入、剪贴板、麦克风与移动端 UI | 当前仓库 |
| Sunshine | Windows 主机、原生触控板注入、剪贴板和麦克风接收 | [silverpoetry/Sunshine](https://github.com/silverpoetry/Sunshine) |
| Moonlight Qt | Windows 客户端、桌面输入和对称文件剪贴板 | [silverpoetry/moonlight-qt](https://github.com/silverpoetry/moonlight-qt) |
| moonlight-common-c | 三端共享的能力协商、输入与传输协议实现 | [固定修订 `ac7f234`](https://github.com/silverpoetry/moonlight-common-c/tree/ac7f2345879070a924b5f8cb339cd0fe25230012) |

触控板、麦克风和新版剪贴板能力均通过协议协商启用。未协商的扩展不会被伪装成旧协议；协议行为的事实来源是仓库固定的 [moonlight-common-c 子模块](app/src/main/jni/moonlight-core/moonlight-common-c)、协议结构和互操作测试，而不是另行编写、可能失效的非官方“协议文档”。

## 核心能力

### 输入与触控

- **原生触控板帧**：双指及多指操作保留真实 pointer ID、归一化位置、按键状态和完整的按下/移动/抬起生命周期，由支持该能力的 Sunshine 注入为主机原生触控板事件。
- **明确的鼠标语义**：普通鼠标、绝对鼠标和触控板模式使用各自的标准路径；单指移动不会因为多指手势结束而产生尾随跳动。
- **低延迟本地光标**：绘制位置与实际发送的单指绝对位置或相对位移共享同一个规范化控制器，无需等待主机回传光标位置。
- **重按与长按**：支持气压计融合重按、可配置压力和最短持续时间阈值、按住拖动及单双指按下；未启用气压计时使用可配置的触控板长按。
- **多指仲裁**：三指呼出键盘只拦截满足轻点条件的候选手势；普通触控板手势、重按和长按可取消候选，关闭该功能时不引入额外输入延迟。

### 剪贴板、文件与麦克风

- **统一剪贴板能力**：一个开关管理文本、PNG、文件和文件夹同步。
- **按需文件传输**：复制文件时只交换轻量清单或占位信息；只有另一端明确粘贴、拉取或分享时才读取目录并传输内容，避免大目录阻塞输入通道。
- **双向文件工作流**：串流菜单可拉取主机剪贴板文件；Android 分享菜单可将本地文件推送到主机桌面。
- **麦克风上行**：复用协商后的音频 UDP 通道，支持串流中启停、静音以及单声道/双声道采集适配，不额外开放一套固定端口。

### 移动端体验

- 可视化整理串流返回菜单，将快捷操作、键盘指令、显示、输入和文件拉取按职责分组。
- 支持虚拟手柄、自定义虚拟按键布局、DS4/DS5/Switch Pro USB 输入、震动与运动传感器适配。
- 支持自定义分辨率、码率、帧率、竖屏、外接显示器、HDR、性能信息和本地视频隐藏等串流选项。
- 类型化设置迁移保留旧版本有效配置；适合即时生效的选项直接更新运行态，需要新会话的选项明确在下一次连接生效。

## 重构后的架构

项目采用“平台无关核心 + Android 适配层 + 原生协议层”的结构。Gradle 在构建阶段执行依赖边界检查，核心模块不能反向依赖 `app` 或 Android UI。

```text
app
 |---> core:hosts
 |---> core:settings ---> core:virtual-controls
 |---> core:transfer
 +---> core:virtual-controls

app/JNI ---> pinned moonlight-common-c ---> Sunshine
```

| 模块 | 负责 | 不负责 |
| --- | --- | --- |
| `app` | Android UI、生命周期组合、数据库/mDNS/NvHTTP 适配器、SAF/URI、原生传输和打包 | 定义跨平台业务契约 |
| `core:hosts` | 不可变主机模型、发现/配对/可达性契约、退出和取消配对用例 | Android 数据库或网络实现 |
| `core:settings` | `SettingKey<T>`、默认值、校验、版本迁移、事务和不可变快照 | Preference 控件和 Activity 导航 |
| `core:transfer` | 文件清单线协议、校验/编码和传输生命周期 | Android URI、进度 UI 或 NvHTTP 实现 |
| `core:virtual-controls` | 虚拟控件布局值对象、文档和仓库契约 | 具体 View 绘制 |
| `core:shield-controller-extensions` | NVIDIA Shield 外设兼容库 | 通用输入状态管理 |
| `build-logic` | 构建约束、架构守卫、依赖和发布策略 | 产品运行逻辑 |

### 关键设计规则

- 每个子系统只有一个可变状态所有者，对外发布不可变快照或显式命令。
- Activity/Fragment 只负责把系统事件翻译为意图并渲染状态，不直接实现协议、文件遍历或业务状态转换。
- 串流会话遵循 `create → start → active → stop → destroy`，清理必须幂等，停止后的迟到回调必须被拒绝。
- 实时输入和音频路径不得阻塞、访问磁盘、创建无界队列或按事件刷 Release 日志。
- 设置采用单向数据流：UI 意图 → 类型化 mutation → repository/migration → 不可变 snapshot → runtime consumer。
- 输入事件由规范化控制器产生一次，再同时提供给本地预测光标和远端 sender，避免两套算法逐渐漂移。
- 文件清单、能力协商和真正的数据读取分离，剪贴板通知不能占用鼠标键盘的实时处理线程。

### 当前重构状态

[重构路线图](docs/architecture/REFACTORING_ROADMAP.md) 的第 1–9 阶段已经完成；第 10 阶段（剩余技术债、发布治理和持续质量收敛）仍在进行。项目不会把“页面看起来完成”当作架构重构全部结束。

更详细的设计依据：

- [总架构约束](ARCHITECTURE.md)
- [Gradle 模块边界](docs/architecture/GRADLE_MODULE_BOUNDARIES.md)
- [设置架构](docs/architecture/SETTINGS_ARCHITECTURE.md)
- [串流会话生命周期](docs/architecture/STREAM_SESSION_LIFECYCLE.md)
- [实时线程规则](docs/architecture/REALTIME_THREADING.md)
- [输入行为基线](docs/architecture/INPUT_BEHAVIOR_BASELINE.md)
- [视口与坐标系统](docs/architecture/STREAM_VIEWPORT_GEOMETRY.md)
- [Android 安全边界](docs/architecture/ANDROID_SECURITY_BOUNDARIES.md)

## 下载与迁移

从 [GitHub Releases](https://github.com/silverpoetry/moonlight-android/releases) 下载：

- `Moonlight-Android-nonRoot-*.apk`：推荐版本，适用于普通 Android 设备。
- `Moonlight-Android-root-*.apk`：仅用于确实需要 root 变体包名或相关能力的设备。

正式包名为 `com.moonlight.android`（root 变体为 `com.moonlight.android.root`），应用名称为 **Moonlight**。Release 构建保持未混淆，便于诊断原生输入和设备兼容问题。

旧版 `com.limelight.unofficialA` 与当前包名不同，不能直接覆盖安装。需要保留旧版主机、证书和密钥时，应按 Release 迁移说明先运行一次性迁移桥，再安装当前 APK；迁移完成后可卸载旧包和迁移桥。

## 构建与质量门禁

```powershell
git submodule update --init --recursive
.\gradlew.bat verifyLocal --rerun-tasks --max-workers=1 --no-daemon
```

`verifyLocal` 会执行模块测试、app JVM 测试、架构检查、四个变体的 Android Lint、依赖与安全策略校验、SBOM 生成，并构建未混淆的 root/non-root Release APK。设备验证方法见 [TESTING.md](TESTING.md)，发布流程见 [Release Runbook](docs/architecture/RELEASE_RUNBOOK.md)，依赖来源见 [DEPENDENCIES.md](DEPENDENCIES.md)。

## 隐私与安全

- 剪贴板和麦克风仅在串流会话中、经双方能力协商后启用。
- 文件复制不会在按下复制键时扫描或上传整个目录；数据只在远端明确请求后读取。
- Release 日志不得包含剪贴板正文、文件路径、证书、密钥或麦克风采样。
- 配对凭据保存在 Android 应用私有存储中，迁移接口使用签名校验与一次性读取约束。

## 作者、贡献者与许可

本项目的版权不是由当前维护者单独拥有。Moonlight 上游作者、Axixi 分支作者、当前维护者以及外部代码来源均列于 [CONTRIBUTORS.md](CONTRIBUTORS.md)；完整逐提交归属以 Git 历史为准。GitHub 的动态 [Contributors 图表](https://github.com/silverpoetry/moonlight-android/graphs/contributors) 仅是辅助视图，缓存重算期间可能暂时为空。

项目整体许可见 [GPL-3.0 `LICENSE.txt`](LICENSE.txt)。依赖锁定和来源见 [DEPENDENCIES.md](DEPENDENCIES.md)，随包提供的第三方许可文本位于 [`app/src/main/assets/third_party_licenses`](app/src/main/assets/third_party_licenses)。任何来源署名都不表示原作者或 Moonlight 官方为本分支背书。
