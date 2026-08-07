# Android Release Quality Baseline

本文件记录当前独立分支的发布基线。历史 SDK 迁移和旧版本产物保存在
[QUALITY_MIGRATION_LOG.md](QUALITY_MIGRATION_LOG.md)，不与当前候选包混在同一张表中。

## 当前候选

- 记录日期：2026-08-07
- 功能提交：`23aa54ae Refine configuration archive import and export`
- 分支：`refactor/compose-material3`
- 版本名：`12.1-260803`
- 版本号：`315`
- common-c：`ac7f2345879070a924b5f8cb339cd0fe25230012`

## 平台与工具链

| 项目 | 当前值 |
| --- | --- |
| 最低 Android API | 23 |
| `compileSdk` / `targetSdk` | 37 / 37 |
| Android Gradle Plugin | 9.3.1 |
| Gradle Wrapper | 9.6.1 |
| 构建 JDK | Eclipse Temurin 21.0.12 |
| Java 字节码目标 | 11 |
| Android NDK | 27.0.12077973 |
| Compose / Material 3 | 由 `gradle/libs.versions.toml` 集中管理 |

## Release 配置

| 变体 | 包名 | 代码与资源处理 |
| --- | --- | --- |
| `nonRootRelease` | `com.silverpoetry.moonlight` | 完整 R8 优化、混淆、代码收缩、资源收缩 |
| `rootRelease` | `com.silverpoetry.moonlight.root` | 完整 R8 优化、混淆、代码收缩、资源收缩；受产品最高 API 限制 |

Debug 变体保持可读类名和资源，用于交互式诊断。Release 日志遵守生产日志策略，实时
输入、音频、剪贴板和文件内容不会逐事件输出。

## 当前 non-root 产物

| 项目 | 值 |
| --- | --- |
| 文件 | `app/build/outputs/apk/nonRoot/release/app-nonRoot-release.apk` |
| 大小 | 25,309,011 bytes |
| SHA-256 | `5084550D27B12783B2812494BFA9C1896D159044CA8278772A6092969D0958AF` |
| R8 mapping SHA-256 | `4D4243048913B30B65340C1F99D21A03E9B6DF63FDEFB44EC03A11C75947427D` |

R8 mapping 位于 `app/build/outputs/mapping/nonRootRelease/mapping.txt`，发布归档必须将
它与对应 APK、版本信息、签名摘要和校验值一起保存。

## 已执行验证

当前候选已执行：

- `ConfigurationArchiveManifestTest`；
- `SettingsDocumentRequestStateTest`；
- `testNonRootReleaseUnitTest`；
- `assembleNonRootRelease`；
- `verifyNonRootRelease --no-daemon`；
- Release Lint、R8、资源收缩、四 ABI native 构建、架构、安全和依赖策略检查。

2026-08-07 的本次门禁通过，共 153 个任务（37 个执行、116 个复用已有结果）。后续源码、
依赖、构建配置或发布产物发生变化后，应重新执行同一命令并更新本节摘要：

```powershell
.\gradlew.bat verifyNonRootRelease --no-daemon
```

设备覆盖安装验证过的实体设备为 `192.168.0.125:5555`、`192.168.3.79:5555` 和
`192.168.3.3:41305`。设备安装验证只证明覆盖安装、包名、版本和数据保留；真实串流、
输入、剪贴板、文件、麦克风和窗口矩阵按 [TESTING.md](TESTING.md) 单独记录。

## 基线使用方式

质量比较应同时记录源码提交、common-c 修订、构建命令、APK 哈希、mapping 哈希、测试
报告和设备限制。历史记录中的旧 SDK、旧包名、旧 R8 策略和旧 APK 哈希只用于解释迁移，
不能作为当前 Release 的验收依据。
