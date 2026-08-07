# 测试与发布前验证

本文件定义当前分支的验证层级和证据要求。构建成功只说明产物能够生成；串流、输入、
窗口、剪贴板、文件和麦克风仍需要在对应设备与主机组合上验证。

## 日常构建

日常开发和安装使用普通 non-root Release：

```powershell
.\gradlew.bat nonRootRelease
```

提交前运行单变体质量门禁：

```powershell
.\gradlew.bat verifyNonRootRelease --no-daemon
```

该门禁覆盖 core 模块测试、non-root Release 单元测试、Release Lint、架构和安全策略、
依赖与原生依赖校验、资源收缩、R8 处理以及四个原生 ABI 的 Release 构建。门禁不跳过
Lint，也不把 Debug 产物当作 Release 证据。

完整本地矩阵：

```powershell
.\gradlew.bat verifyLocal --no-daemon
```

完整矩阵包含 root/non-root 的 JVM 测试、各变体 Lint、两种 Release 产物、原生构建、
安全与依赖检查以及 SBOM。Lint 没有项目级基线；只有有明确原因的单点例外，并且例外
必须紧邻代码说明。社区翻译尚未完成，因此 `MissingTranslation` 是唯一保留的项目级
禁用检查。

## 设备验证

连接一台设备并设置 `ANDROID_SERIAL`：

```powershell
$env:ANDROID_SERIAL = "device-address:5555"
.\gradlew.bat verifyConnected --no-daemon
```

该命令先执行本地门禁，再运行 root 和 non-root Debug instrumentation。没有可用设备或
任一测试失败时，命令失败。开发过程中可以直接运行单个 instrumentation 类，但这类
定向运行不能替代完整的 `verifyConnected`。

Release 覆盖安装还需要记录：设备序列号、旧版本包名、安装前后版本、应用数据是否保留、
首次启动是否成功以及 AndroidRuntime 是否出现异常。涉及包名迁移时，必须单独验证导出
配置、安装新包、导入配置和重新配对的路径。

## 功能验收矩阵

按变更范围执行相应项目，并在提交或发布证据中注明设备、Android 版本、主机端版本和
结果。未执行的项目写明“未验证”，不使用“应该可以”代替结果。

| 范围 | 重点场景 |
| --- | --- |
| 主机 | mDNS 发现、手动/远程地址、配对、证书固定、断线重连、应用列表和应用启动 |
| 串流 | 首次连接、停止/恢复、Surface/解码器、横竖屏、自由窗口、分屏、画中画、HDR、后台恢复 |
| 输入 | 鼠标、绝对定位、触控板、多指、双指右键、三指键盘、长按/重按、本地光标、虚拟控件和手柄 |
| 传输 | 文本、PNG、文件、目录、Unicode、空文件、冲突、取消、重试、双向和大目录按需传输 |
| 麦克风 | 单声道/双声道、协商格式、启停、静音、重连、长时间采集和回调过载 |
| 界面 | 手机/平板、横竖屏、分屏/自由窗口、深浅色、动态取色、设置滚动位置、返回菜单编辑和启动页 |
| 安全 | Release manifest、自动备份排除、SAF URI、证书/私钥保护、日志脱敏、依赖许可证和 SBOM |

### 配置 ZIP v2

每次修改备份与恢复、设置 schema、主机数据库或客户端身份时，至少执行以下矩阵：

1. 使用系统“另存为”界面导出 ZIP，确认可以修改文件名和保存位置，取消操作不会留下临时文件。
2. 检查 ZIP manifest 的格式版本、组件清单、文件大小和 SHA-256；损坏摘要、未知路径、
   超限文件和不匹配的证书/私钥必须在写入前拒绝。
3. 分别只导入 App 设置、主机连接信息和客户端身份，确认未选择的组件保持不变。
4. 只导入主机连接信息时，主机名称、地址、端口、MAC 和固定证书恢复，当前客户端身份
   保留；刷新主机后按实际配对状态显示“需要配对”或“已配对”。
5. 导入客户端身份时，证书和私钥作为一个原子组件写入；任一文件缺失或签名匹配失败时，
   两个文件都不替换。
6. 让主机数据库合并故意失败，确认 SQLite 事务不留下半条主机记录；让设置或身份写入
   之后的后续步骤失败，确认对应的设置快照和身份文件恢复到导入前状态。
7. 在已有主机、已有配对和自定义设置的真实设备上覆盖安装，再重复一次完整导出/导入。

## 原生协议测试

`moonlight-common-c` 是 Git 子模块，拥有独立的 CMake/CTest 套件。Android 的 `ndk-build`
只编译该库，不执行主机侧 native 测试；共享协议发生变化时，应在子模块目录运行其
CTest，并记录固定修订和结果。MinGW/UCRT 环境运行 CTest 时，需要把编译器运行库的
`bin` 目录加入 `PATH`；退出码 `0xc0000135` 表示运行库 DLL 不在 PATH，不代表断言失败。

## 发布证据

每个发布候选归档以下内容：

- Android、Moonlight Qt、Sunshine 和 common-c 的提交 ID；
- Gradle/JDK/NDK/SDK 版本和构建命令；
- APK、R8 mapping、签名证书摘要、SHA-256 和 SBOM；
- unit test、Lint、instrumentation 和功能矩阵报告；
- 设备型号、Android 版本、主机端版本、网络条件和未验证项。

Release 日志必须保持有界，不包含剪贴板正文、文件路径、证书、私钥、麦克风采样或逐
事件实时轨迹。出现被忽略的失败、无法解释的警告、脏的生成文件、缺少 mapping 或缺少
关键矩阵证据时，候选版本不应发布。
