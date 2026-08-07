# 第三方声明

Moonlight for Android 建立在 Moonlight 社区长期维护的代码和协议实现之上，并包含来自
其他开源项目的移植与技术参考。本文件提供分发层面的索引；逐提交作者、具体来源修订和
维护关系见 [CONTRIBUTORS.md](CONTRIBUTORS.md)，完整依赖策略见
[DEPENDENCIES.md](DEPENDENCIES.md)。

## 项目传承

- [moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android)：
  Android 客户端基础、GameStream 协议接入和长期上游维护。
- [Axixi2233/moonlight-android](https://github.com/Axixi2233/moonlight-android)：
  本分支继承的移动端界面、输入、手柄、显示和配置能力来源。
- [moonlight-stream/moonlight-common-c](https://github.com/moonlight-stream/moonlight-common-c)：
  Moonlight 客户端共享的原生协议实现；当前仓库使用固定子模块修订。

相关作者和版权仍属于各自贡献者。本项目的独立名称、包名和维护方向不改变历史代码的
许可与署名义务。

## 受控依赖

| 组件 | 版本 | 用途 | 许可证位置 |
| --- | --- | --- | --- |
| OpenSSL | 3.5.7 LTS | TLS 与协议加密 | `app/src/main/assets/third_party_licenses/openssl-3.5.7.txt` |
| libopus | 1.6.1 | 串流音频解码与麦克风编码 | `app/src/main/assets/third_party_licenses/libopus-1.6.1.txt` |
| ShieldControllerExtensions | 1.0.1 | NVIDIA SHIELD 手柄扩展 | `app/src/main/assets/third_party_licenses/shield-controller-extensions-1.0.1.txt` |

Gradle 依赖和传递依赖记录在 CycloneDX SBOM 中；发布前由依赖校验任务固定下载产物摘要。

## 已记录的移植与技术参考

- DualSense USB 驱动移植自 [Geocld/PeaSyo](https://github.com/Geocld/PeaSyo)，原项目
  声明为 AGPL-3.0；相关代码继续受适用的原始条款约束。
- Nintendo Switch Pro USB 输入来自
  [ClassicOldSong/moonlight-android](https://github.com/ClassicOldSong/moonlight-android)，
  该仓库声明为 GPL-3.0。
- Qualcomm/MediaTek 低延迟解码规则保留 ALONSOJR1980 的署名。
- DualSense 报文行为参考 pydualsense、公开报文记录和 dualsense-tester；具体链接与提交
  记录见 [CONTRIBUTORS.md](CONTRIBUTORS.md)。

引入新的第三方代码或资源时，应在合入前确认许可证兼容性、保留版权声明、记录固定来源，
并把二进制分发要求附带的许可证原文加入安装包。

## 商标

Moonlight、Sunshine、Android、NVIDIA、PlayStation、Nintendo 以及设备和厂商名称仅用于
说明兼容性，商标归各自权利人所有。本项目与这些品牌不存在官方隶属或背书关系。
