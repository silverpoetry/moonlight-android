# Android Release Quality Baseline

本文件记录当前独立分支的发布基线。历史 SDK 迁移和旧版本产物保存在
[QUALITY_MIGRATION_LOG.md](QUALITY_MIGRATION_LOG.md)，不与当前候选包混在同一张表中。

## 当前候选

- 记录日期：2026-08-07
- 构建候选提交：`d7420a25 Prepare v12.1-260807 release`
- 分支：`master`
- 版本名：`12.1-260807`
- 版本号：`316`
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

## 当前 Release 产物

| 项目 | 值 |
| --- | --- |
| non-root 文件 | `app/build/outputs/apk/nonRoot/release/app-nonRoot-release.apk` |
| non-root 大小 | 25,308,996 bytes |
| non-root APK SHA-256 | `3AE804821D97E8F0781C09538804C956974D8769DB5EC3CB94AAC7857D2CDB3C` |
| non-root mapping SHA-256 | `4D4243048913B30B65340C1F99D21A03E9B6DF63FDEFB44EC03A11C75947427D` |
| root 文件 | `app/build/outputs/apk/root/release/app-root-release.apk` |
| root 大小 | 25,391,652 bytes |
| root APK SHA-256 | `A39B739AE865A2717B9DFDEB30F6774FFA41CAB610E95D1405F2337A0C1EBCF5` |
| root mapping SHA-256 | `1B508EBF9C700AD8CE976494CEFF094A80ED5B3C3704E87F5CE8C93F0DFCCD54` |
| SBOM | `build/reports/sbom/moonlight-android.cdx.json`，2,212,055 bytes，SHA-256 `DBB5DDD7CE66042B43904907C1DD7CD0D71C3A1B66B71367F61460FB1E746BED` |
| non-root 包名 / 版本 | `com.silverpoetry.moonlight` / `316` / `12.1-260807` |
| root 包名 / 版本 | `com.silverpoetry.moonlight.root` / `316` / `12.1-260807` |
| non-root ABI | `armeabi-v7a`、`arm64-v8a`、`x86`、`x86_64` |
| 签名证书 SHA-256 | `DB56911E119D53D8B67EFBF6607FE7D19BD0B2ACECEB6B3FB7D5F902EAEAC743`，与 `v12.1-260803` 一致 |

R8 mapping 位于 `app/build/outputs/mapping/<variant>Release/mapping.txt`。发布归档将它、
对应 APK、版本信息、签名摘要和校验值一起保存；GitHub Release 随 APK 附带 SHA-256 清单。

## 已执行验证

2026-08-07 已执行：

- `ConfigurationArchiveManifestTest`；
- `SettingsDocumentRequestStateTest`；
- `testNonRootReleaseUnitTest`；
- `assembleNonRootRelease`；
- `verifyLocal --rerun-tasks --no-parallel`，`BUILD SUCCESSFUL`，351 个任务全部重新执行；
- Release Lint、R8、资源收缩、四 ABI native 构建、架构、安全和依赖策略检查；
- CycloneDX SBOM 生成和格式/版本核对；
- APK 签名、包名、版本、四 ABI 和 mapping 摘要核对。

后续源码、依赖、构建配置或发布产物发生变化后，应重新执行同一命令并更新本节摘要：

```powershell
.\gradlew.bat verifyLocal --rerun-tasks --no-parallel
```

设备覆盖安装验证过的实体设备为 `192.168.0.125:5555`、`192.168.3.79:5555` 和
`192.168.3.3:41305`。设备安装验证只证明覆盖安装、包名、版本和数据保留；真实串流、
输入、剪贴板、文件、麦克风和窗口矩阵按 [TESTING.md](TESTING.md) 单独记录。

## 基线使用方式

质量比较应同时记录源码提交、common-c 修订、构建命令、APK 哈希、mapping 哈希、测试
报告和设备限制。历史记录中的旧 SDK、旧包名、旧 R8 策略和旧 APK 哈希只用于解释迁移，
不能作为当前 Release 的验收依据。
