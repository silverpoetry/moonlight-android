# 作者、贡献者与代码来源

Moonlight for Android 来自多个仓库和社区的长期工作。本文件把项目传承永久保存在版本库中，避免署名依赖 GitHub 的 Contributors 缓存、账号是否仍然可用或某个特定网页界面。

逐提交作者以 Git 历史为最终依据。下列名单说明可确认的维护关系和功能来源，不取代版权声明，也不取代相应来源的许可证条款。

## 项目传承

| 阶段 | 项目 | 作者与作用 |
| --- | --- | --- |
| 官方上游 | [moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android) | 提供 Android 客户端基础和当前历史中的大部分代码。上游保留的主要作者包括 [Cameron Gutman](https://github.com/cgutman)、[Diego Waxemberg](https://github.com/dwaxemberg)、Aaron Neyer 和 [Andrew Hennessy](https://github.com/yetanothername)。完整名单见[上游贡献历史](https://github.com/moonlight-stream/moonlight-android/graphs/contributors)。 |
| Axixi 分支 | [Axixi2233/moonlight-android](https://github.com/Axixi2233/moonlight-android) | [Axixi2233](https://github.com/Axixi2233) 及该分支贡献者开发了本分支继承的移动端 UI、输入、手柄、显示和配置能力。Axixi 的公开主页包括 [Bilibili](https://space.bilibili.com/16893379) 和 [YouTube](https://www.youtube.com/@AxixiTV)。 |
| 当前分支 | [silverpoetry/moonlight-android](https://github.com/silverpoetry/moonlight-android) | [silverpoetry / 霜冷长河](https://github.com/silverpoetry) 维护独立重构、Compose/Material 3 界面、共享协议接入、原生触控板、剪贴板/文件传输、麦克风上行、设置架构和发布治理。 |

GitHub 记录的本仓库源头是 Moonlight Android 官方仓库，直接父仓库是 Axixi 分支。修改包名、资源和架构不会抹去这段历史。

## 可确认的外部代码与技术参考

以下来源由提交信息或源码注释明确记录。只要相关代码继续被分发或复用，这些来源就不得被删除：

| 范围 | 来源或贡献者 | 记录 |
| --- | --- | --- |
| DualSense USB 驱动 | [Geocld/PeaSyo](https://github.com/Geocld/PeaSyo) | 提交 `6825ef56` 明确记录 DS5 驱动移植自 PeaSyo。PeaSyo 声明为 AGPL-3.0；分发和修改衍生代码时仍须遵守适用的原始条款。 |
| Nintendo Switch Pro USB 输入 | [ClassicOldSong/moonlight-android](https://github.com/ClassicOldSong/moonlight-android) | 提交 `024bcb59` 记录代码合入来源；该仓库声明为 GPL-3.0。 |
| Qualcomm/MediaTek 低延迟解码规则 | [ALONSOJR1980](https://github.com/alonsojr1980) | 提交 `a31fb69a` 和 `MediaCodecHelper` 中保留的注释均有署名。 |
| 多点触控灵敏度优化 | XMJL | 提交 `6f75fa0c` 对代码片段提供者进行了署名；仓库中没有可验证的公开账号链接。 |
| DualSense 报文行为 | [pydualsense](https://github.com/flok/pydualsense)、[stealth-alex 的报文记录](https://gist.github.com/stealth-alex/10a8e7cc6027b78fa18a7f48a0d3d1e4) 和 [dualsense-tester](https://github.com/daidr/dualsense-tester) | 提交 `6825ef56` 与当前源码注释记录的技术参考；本项目不将这些参考材料宣称为自己的原创内容。 |
| NVIDIA Shield 手柄扩展 | NVIDIA / Android Open Source Project 贡献者 | 受控源码依赖和 MIT 声明记录于 [DEPENDENCIES.md](DEPENDENCIES.md) 与 [`shield-controller-extensions-1.0.1.txt`](app/src/main/assets/third_party_licenses/shield-controller-extensions-1.0.1.txt)。 |

本表只记录已知来源，不表示“写了署名”就自动满足全部许可证要求。引入代码时还必须检查许可证兼容性、保留所需声明，并将要求随二进制提供的许可证文本放入安装包。

## 仓库贡献者

当前分支的可达历史包含大量贡献者，完整名单应以 Git 提交中的作者与共同作者字段为准。
以下账号均是当前历史中可以确认的主要贡献者，作为便于查阅的索引：

- [cgutman](https://github.com/cgutman)
- [silverpoetry](https://github.com/silverpoetry)
- [Axixi2233](https://github.com/Axixi2233)
- [Kaitul](https://github.com/Kaitul)
- [jorys-paulin](https://github.com/jorys-paulin)
- [dwaxemberg](https://github.com/dwaxemberg)
- [ZerOri](https://github.com/ZerOri)
- [Ansa89](https://github.com/Ansa89)
- [yetanothername](https://github.com/yetanothername)
- [bubuleur](https://github.com/bubuleur)
- [unforced](https://github.com/unforced)
- [unbiaseduser-github](https://github.com/unbiaseduser-github)

这不是贡献门槛，也不是完整作者名单。完整记录应从以下位置查询：

- [当前分支 Contributors 图表](https://github.com/silverpoetry/moonlight-android/graphs/contributors)
- [Axixi 分支 Contributors 图表](https://github.com/Axixi2233/moonlight-android/graphs/contributors)
- [Moonlight 官方 Contributors 图表](https://github.com/moonlight-stream/moonlight-android/graphs/contributors)
- 本地 Git 历史：`git shortlog -sne --all`

GitHub 的 Contributors 图表由平台异步生成，偶尔会在仓库建立或历史变化后暂时为空。
图表是浏览入口，不是署名依据；Git 提交、源码版权声明和本文件共同保存可审计的来源记录。

## 维护署名的方法

引入或改写其他项目的代码时：

1. 在可行时保留原始提交作者，并在提交信息中写明来源仓库和固定修订。
2. 将来源、作者、受影响范围和许可证加入本文件；若属于受控依赖，同时更新 [DEPENDENCIES.md](DEPENDENCIES.md)。
3. 将二进制分发要求附带的第三方许可证原文放入 `app/src/main/assets/third_party_licenses`。
4. 不得用一句笼统的“基于 Moonlight”替代具体署名，也不得暗示上游作者为本分支背书。
5. 合并前检查许可证兼容性；事后补文档不能替代这项检查。

## 许可

仓库顶层许可为 [GPL-3.0](LICENSE.txt)。个别移植或受控依赖可能带有上表和 [DEPENDENCIES.md](DEPENDENCIES.md) 所述的附加或更强义务。版权仍属于各自作者。
