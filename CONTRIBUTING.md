# 参与贡献

感谢参与 Moonlight for Android 的开发。这个项目同时涉及实时输入、视频解码、网络协议、
Android 生命周期和用户配置，变更需要先说明行为边界，再选择实现方式。

## 开发环境

- JDK 21；
- Android SDK 37；
- Android NDK `27.0.12077973`；
- Git 及完整子模块。

初始化并运行日常门禁：

```powershell
git submodule update --init --recursive
.\gradlew.bat verifyNonRootRelease --no-daemon
```

完整本地矩阵使用 `verifyLocal`，连接设备验证使用 `verifyConnected`。详细范围见
[TESTING.md](TESTING.md)。

## 设计规则

1. 一个可变状态只有一个生产所有者，其他组件通过不可变快照、窄接口或语义事件访问。
2. Compose 负责非实时展示；视频 Surface、解码器、输入、音频和 JNI 保持在专门的实时链路。
3. core 模块不依赖 Android、Activity、View、Compose、SharedPreferences、NvHTTP 或 JNI 类型。
4. 输入、音频、视频和文件传输使用有界队列，明确取消、销毁和迟到回调的处理方式。
5. 设置由类型化 schema、默认值、校验和迁移统一管理；运行态不直接读取 SharedPreferences。
6. 文件复制只发布轻量元数据，内容读取由明确的粘贴、拉取或分享动作触发。
7. 新的协议能力需要能力协商、版本边界、失败语义和参与端测试，不能只修改单端行为。
8. 先用测试或复现记录固定外部行为，再移动所有权或删除旧路径。

完整架构和依赖方向见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 提交要求

- 提交信息使用简短、明确的祈使句；
- 一个提交只承担一个可审查的职责，结构迁移和行为变化尽量分开；
- 涉及协议、设置迁移、权限、用户数据或生命周期时同步更新对应文档；
- 不提交 APK、构建目录、签名文件、私钥、真实配置 ZIP、剪贴板内容、设备地址或未脱敏日志；
- 引入外部代码时保留作者和许可证，记录固定来源修订，并更新
  [CONTRIBUTORS.md](CONTRIBUTORS.md) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)；
- PR 或变更说明应包含影响范围、测试命令、实机环境、结果和仍未验证的场景。

## 领域验证

界面变更至少覆盖手机/平板、横竖屏、深浅色和窗口尺寸变化；输入变更覆盖取消、焦点丢失
和最后一指抬起；串流变更覆盖连接中取消、后台/前台、Surface 重建和迟到回调；文件与
配置迁移覆盖损坏输入、取消、重试和部分失败。涉及公共协议时，同一批次验证 Android、
Moonlight Qt、Sunshine 和固定的 common-c 修订。

硬件或系统行为未经验证时，请准确标为“未验证”或写明证据层级。

## 许可

提交贡献即表示你有权按仓库适用的 GNU GPL 条款提供该内容。代码来源和第三方许可证应
在合入前确认；提交后的署名补充不能替代许可证兼容性检查。
