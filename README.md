<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_app.png" width="128" alt="Moonlight icon">
  <h1>Moonlight for Android</h1>
  <p>面向远程桌面与高质量串流输入的 Moonlight Android 独立分支</p>

  [![Release](https://img.shields.io/github/v/release/silverpoetry/moonlight-android?style=flat-square)](https://github.com/silverpoetry/moonlight-android/releases)
  [![License](https://img.shields.io/github/license/silverpoetry/moonlight-android?style=flat-square)](LICENSE.txt)
  [![Android](https://img.shields.io/badge/Android-5.0%2B-3DDC84?logo=android&logoColor=white&style=flat-square)](https://developer.android.com/)
</div>

本项目基于 [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) 持续维护，重点解决移动设备作为远程桌面触控板、剪贴板终端和麦克风输入端时的完整体验。它不是 Moonlight 官方发行版；协议扩展需要配套的 [Sunshine](https://github.com/silverpoetry/Sunshine) 主机端。

## 核心能力

- **原生触控板输入**：通过协商后的多点触控板帧发送手指、按键和多指手势；普通鼠标与触控板模式保持各自明确的输入语义。
- **低延迟本地光标**：本地光标与实际发送的单指移动共用同一套位置控制，避免等待主机回传造成的视觉延迟。
- **重按与长按**：支持气压计融合重按、可配置阈值和按住拖动；不支持气压计时使用可配置的触控板长按。
- **统一剪贴板**：一个开关管理文本、PNG、文件和文件夹；文件仅发布轻量占位与能力信息，在另一端真正粘贴或拉取时才按需传输。
- **麦克风上行**：复用加密音频传输，支持串流中启停、静音及单/双声道输入适配。
- **可维护的设置架构**：类型化设置、一次性迁移、即时生效，以及为手机、平板、横屏和外接显示器整理的分组界面。
- **可定制串流菜单**：快捷操作、键盘指令、虚拟手柄、性能信息、画面显示和文件拉取可按需要组织。
- **输入设备支持**：虚拟手柄、自定义按键布局、DS4/DS5/Switch Pro USB 支持、震动与运动传感器适配。

## 配套项目

| 组件 | 职责 | 推荐分支 |
| --- | --- | --- |
| [Sunshine](https://github.com/silverpoetry/Sunshine) | Windows 串流主机、触控板注入、剪贴板与麦克风接收 | `master` |
| [Moonlight Qt](https://github.com/silverpoetry/moonlight-qt) | Windows 桌面客户端和对称剪贴板文件传输 | `master` |
| [moonlight-common-c](https://github.com/silverpoetry/moonlight-common-c) | 三端共享的能力协商与协议实现 | `unified` |

触控板、麦克风和新版剪贴板能力均通过协议协商启用。未协商的扩展不会被伪装成旧协议；为获得完整功能，请让上述组件使用同一套发布版本。

## 下载与安装

从 [GitHub Releases](https://github.com/silverpoetry/moonlight-android/releases) 下载：

- `Moonlight-Android-nonRoot-*.apk`：推荐版本，适用于普通 Android 设备。
- `Moonlight-Android-root-*.apk`：仅用于确实需要 root 变体包名或相关能力的设备。

正式包名为 `com.moonlight.android`（root 变体为 `com.moonlight.android.root`），应用名称为 **Moonlight**。Release 构建保持未混淆，方便诊断原生输入和设备兼容问题。

旧版 `com.limelight.unofficialA` 与当前包名不同，不能直接覆盖安装。需要保留旧版主机、证书和密钥的用户，应按 Release 中的迁移说明先安装一次性迁移桥，再安装当前 APK；迁移完成后可卸载旧包和迁移桥。

## 构建与验证

```powershell
git submodule update --init --recursive
.\gradlew.bat verifyLocal --rerun-tasks --max-workers=1 --no-daemon
```

`verifyLocal` 会执行全部 JVM 测试、架构检查、四个变体的 Android Lint、依赖与安全策略校验、SBOM 生成，并构建未混淆的 root/non-root Release APK。连接设备后的完整验证方法见 [TESTING.md](TESTING.md)，正式发布门禁见 [Release Runbook](docs/architecture/RELEASE_RUNBOOK.md)。

## 隐私与安全

- 剪贴板和麦克风能力仅在串流会话中、经双方能力协商后启用。
- 文件复制不会在按下复制键时扫描或上传整个目录；数据只在远端明确请求后传输。
- 发布日志不应包含剪贴板正文、文件路径、证书、密钥或麦克风采样。
- 配对凭据保存在 Android 应用私有存储中，迁移接口使用签名校验与一次性读取约束。

## 上游与许可

本项目保留 Moonlight 上游的作者、第三方声明和 GPL 授权。详见 [LICENSE.txt](LICENSE.txt) 与 [第三方许可](app/src/main/assets/ThirdPartyLicenses.json)。欢迎提交可复现的问题报告和结构清晰的改进。
