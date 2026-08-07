# Moonlight Android 架构

本文定义当前独立分支的生产架构、依赖方向和状态所有权。新增代码应遵循这些约束；仍在
`app` 中的历史实现则按 [重构路线图](docs/architecture/REFACTORING_ROADMAP.md) 逐步收敛，
每次迁移都应保留可验证的外部行为。

## 设计目标

- 让界面、主机、设置、输入、传输和串流会话拥有清晰的职责边界。
- 让协议可见行为由显式状态机、不可变模型和测试固定下来。
- 让触控、音频、视频和传输回调保持有界、低延迟并可取消。
- 让 Activity 负责 Android 生命周期组合，让业务状态和协议操作由专门的控制器持有。
- 让 Release 构建、依赖、权限、日志和发布证据成为可执行的工程规则。

代码行数和类大小用于发现问题，但不作为重构目标。真正的完成标准是状态所有权明确、
依赖方向单一、失败与取消可恢复、外部行为可验证。

## 总体分层

```text
Android entry points
        │
        ▼
Compose feature UI ── immutable UI state / semantic events
        │
        ▼
feature controllers and use cases
        │
        ▼
platform-independent core contracts and models
        ▲
        │
Android persistence / network / SAF / hardware adapters
        │
        ▼
stream, input and protocol adapters
        │
        ▼
moonlight-common-c / JNI / Sunshine
```

依赖指向业务契约。核心模型不依赖 Activity、Fragment、View、Compose、SharedPreferences、
Android URI、NvConnection 或 JNI 类型。Android 和协议实现通过面向消费者的小接口接入。

## Gradle 模块

```text
app
 |---> core:hosts
 |---> core:settings ---> core:virtual-controls
 |---> core:transfer
 |---> core:virtual-controls
 +---> core:shield-controller-extensions
```

| 模块 | 所有权 |
| --- | --- |
| `app` | Android 入口、Compose 界面、生命周期组合、数据库、mDNS、NvHTTP、SAF/URI、硬件适配、JNI、串流和打包 |
| `core:hosts` | 主机身份、端点、可达性、配对结果、仓库契约和主机操作用例 |
| `core:settings` | `SettingKey<T>`、校验、迁移、不可变设置快照、事务和领域更新 |
| `core:transfer` | 文件清单线协议、校验、编码和传输生命周期状态 |
| `core:virtual-controls` | 虚拟控件布局值对象、文档格式和仓库契约 |
| `core:shield-controller-extensions` | NVIDIA SHIELD 外设兼容实现 |
| `build-logic` | Java 约定、工作树构建锁、依赖策略、架构守卫、安全门禁和发布任务 |

新的物理模块需要满足四个条件：公开契约稳定、包依赖无环、能够独立测试、模块边界能带来
实际的所有权或构建期约束。详细证据见
[GRADLE_MODULE_BOUNDARIES.md](docs/architecture/GRADLE_MODULE_BOUNDARIES.md)。

## 界面架构

非实时界面使用 Jetpack Compose 和 Material 3。`MoonlightTheme` 统一颜色、字体、形状、
深浅色和动态取色策略。各功能渲染不可变页面状态，并向控制器发送语义事件。

主要界面按业务组织：

- `ui.compose.hosts`：主机发现、主机卡片和串流/应用/主机操作入口；
- `ui.compose.apps`：主机应用列表、封面和启动操作；
- `ui.compose.settings`：设置分类、详情页、搜索、滑块、选择器和自适应双栏导航；
- `ui.compose.transfer`：文件上传、下载、目标目录和进度；
- `ui.gamemenu`：串流快捷菜单、可编辑控制卡片和当前会话操作。

Activity 是 Android 组合根，负责窗口、权限、Activity Result、系统回调和控制器生命周期。
它不承担协议编解码、文件遍历、设置解析或跨会话状态。Compose 页面不直接读写
SharedPreferences，也不持有 NvConnection。

视频 `SurfaceView`、解码器、输入、音频、JNI 和串流会话保留在专门的实时链路。串流菜单
作为该链路之上的界面层工作，不改变视频和输入的线程模型。相关决策见
[ADR 0004](docs/adr/0004-compose-material3-presentation.md)。

## 状态所有权

每类可变状态只有一个写入者：

| 状态 | 所有者 |
| --- | --- |
| 活跃触点、手势候选、按键和重按状态 | 输入控制器 |
| 本地预测光标参考位置 | 本地光标控制器 |
| NvConnection 和串流状态 | `StreamSessionController` |
| 窗口、视频矩形和坐标变换 | 视口控制器 |
| Surface、解码器、HDR 和媒体资源 | 渲染资源所有者 |
| 持久设置 | `SettingsRepository` |
| 虚拟手柄/按键布局文档 | 虚拟控件布局仓库 |
| 手柄设备、槽位、传感器和报告 | 手柄子系统 |
| 剪贴板清单和文件传输任务 | 传输控制器 |
| 客户端证书和私钥 | Android 加密适配器 |

其他组件通过不可变快照、只读接口或显式命令观察这些状态。跨层共享可变集合、Activity
字段和通用事件总线不属于生产架构。

## 单向状态流

非实时页面遵循统一的数据流：

```text
user/system event
        -> controller or state holder
            -> domain mutation / use case
                -> immutable UI state
                    -> Compose rendering
```

页面重组只消费状态，不触发协议请求或持久化副作用。一次性副作用由生命周期绑定的
控制器执行，并带有取消、代际或幂等约束。

实时输入、音频和视频回调使用明确的低开销端口和状态机，规则见
[REALTIME_THREADING.md](docs/architecture/REALTIME_THREADING.md)。

## 串流生命周期

一次连接由单个 `StreamSessionController` 管理：

```text
create -> start -> active -> stop -> destroy
```

- `start()` 只接受一次；重连创建新的会话和连接对象。
- `stop()` 在启动前、启动中、运行中和失败后都安全，并且只安排一次清理。
- `destroy()` 先断开 UI 委托和系统回调，再释放输入、Surface、音频、锁和连接资源。
- 迟到回调携带会话代际或在所有者销毁后被拒绝。
- Surface 准备、连接准备和页面切换是不同阶段；耗时的网络与解码准备可以在后台完成，
  实际串流界面只在资源就绪后进入。

完整状态表和资源释放顺序见
[STREAM_SESSION_LIFECYCLE.md](docs/architecture/STREAM_SESSION_LIFECYCLE.md)。

## 输入与坐标

输入捕获先转换为规范化事件，再同时交给本地光标和远端发送器。这样绘制位置与协议位置
共享坐标、裁剪和状态转换，避免重复加速或重复偏移。

- 多点触控以完整帧处理，同一时间点的触点共享状态和生命周期。
- 手势仲裁在一个串行执行域内完成，取消、焦点丢失和最后一指抬起释放所有远端状态。
- 三指键盘候选只在配置启用时建立，长按、重按和其他多指操作可以终止候选。
- 本地光标参考位置由发送路径更新；主机回传只负责权威形状和可用时的状态同步。
- 视口、视频矩形、显示挖孔和窗口坐标转换使用同一个几何模型。

可执行行为基线见 [INPUT_BEHAVIOR_BASELINE.md](docs/architecture/INPUT_BEHAVIOR_BASELINE.md)，
坐标契约见 [STREAM_VIEWPORT_GEOMETRY.md](docs/architecture/STREAM_VIEWPORT_GEOMETRY.md)。

## 设置与配置归档

`SettingsRepository` 是唯一持久化边界。每个 `SettingKey<T>` 定义存储名称、类型、默认值、
校验和迁移别名；运行态消费初始化或更新时生成的不可变设置快照。

配置归档使用版本化 ZIP：

| 组件 | 文件 | 导入语义 |
| --- | --- | --- |
| App 设置 | `app-settings.json` | 通过类型化 schema 校验并事务写入 |
| 主机连接信息 | `paired-hosts.db` | 校验 SQLite 快照后原子合并，不改变客户端身份 |
| 客户端身份 | `client.crt`、`client.key` | 验证证书与私钥匹配后成对写入和回滚 |

ZIP manifest 固定格式版本、组件关系、文件大小和 SHA-256。导入先完成全包结构和摘要校验，
再解压用户选择的组件。App 设置和客户端身份可回滚；主机数据库最后执行，由 SQLite
事务保证单次合并的原子性。Android 文档目录 URI 属于设备权限，不进入可移植设置。

完整设置模型见 [SETTINGS_ARCHITECTURE.md](docs/architecture/SETTINGS_ARCHITECTURE.md)。

## 剪贴板与文件传输

剪贴板通知、文件清单和文件内容属于三个阶段：

```text
clipboard change -> bounded metadata/placeholder -> explicit paste or pull -> payload I/O
```

复制操作不遍历大型目录。真正的 URI 枚举、内容读取、进度和取消运行在独立的有界执行器，
输入线程和串流回调不等待传输。平台层负责 SAF 权限和 URI；`core:transfer` 负责清单格式、
校验和生命周期状态。

## 安全与发布

- 生产组件显式声明 exported 状态，外部入口保持在审核过的白名单内。
- 客户端私钥、主机数据库和固定证书排除 Android 自动备份；显式 ZIP 导出由用户选择具体
  文件 URI。
- Release 使用完整 R8 优化、混淆和资源收缩；Debug 保留可读类名用于诊断。
- Release 日志按策略编译移除或限频，不记录敏感载荷和逐帧实时事件。
- Gradle 仓库、版本、校验和、原生源码摘要、许可证和 SBOM 均由仓库规则管理。

安全边界见 [ANDROID_SECURITY_BOUNDARIES.md](docs/architecture/ANDROID_SECURITY_BOUNDARIES.md)，
发布操作见 [RELEASE_RUNBOOK.md](docs/architecture/RELEASE_RUNBOOK.md)。

## 平台与工具链

- Android 6.0（API 23）及以上；
- `compileSdk` / `targetSdk` 37；
- Java 11 字节码目标，构建 JDK 21；
- Gradle 9.6.1、Android Gradle Plugin 9.3.1；
- Kotlin 2.4.10、Compose BOM 2026.06.01；
- Android NDK `27.0.12077973`；
- 四个原生 ABI：`arm64-v8a`、`armeabi-v7a`、`x86_64`、`x86`。

## 验证门禁

日常普通产品门禁：

```powershell
.\gradlew.bat verifyNonRootRelease --no-daemon
```

完整本地门禁：

```powershell
.\gradlew.bat verifyLocal --no-daemon
```

门禁覆盖核心模块测试、应用 Debug/Release 测试、架构边界、Lint、安全和日志策略、依赖
校验、SBOM、四 ABI 原生构建以及 root/non-root Release。涉及公共协议时还应运行固定
`moonlight-common-c` 修订的 CTest；涉及设备行为时按 [TESTING.md](TESTING.md) 执行
连接设备和实机串流矩阵。

## 变更原则

1. 先用测试或可复现记录固定外部行为。
2. 为新的状态所有者定义窄接口和不可变输入输出。
3. 一次迁移一个职责，结构移动和行为变化分开审查。
4. 新路径接管后，在同一阶段删除被替代的生产实现。
5. 协议、线程、生命周期、安全或用户数据变化必须更新对应文档和验证证据。
6. 每个提交保持可构建，阶段结束通过完整门禁。

历史决策和迁移顺序分别记录在 [docs/adr](docs/adr) 与
[REFACTORING_ROADMAP.md](docs/architecture/REFACTORING_ROADMAP.md)。
