# vivo高刷全开（VivoHfrAlwaysOn）— vivo 全应用高刷模块

> 🔥 让"使用高刷新率的应用"列表彻底退役——所有应用（含新装、含被系统自动置关的）无视开关，一律高刷
>
> @author bomo

> [!IMPORTANT]
> **目前仅在 vivo V2419A（OriginOS 6 · Android 16 · LSPosed 2.2.0）上完整实测可用。**
> 模块钩的是 vivo 框架私有类，其他 OriginOS 版本类名/字段名可能不同——
> 不匹配时钩子**静默跳过**（不生效，但绝不影响开机与系统稳定）。
> 适配反馈请附机型 + `adb logcat | grep VivoHfrAlwaysOn` 输出，随缘更新。

[![Version](https://img.shields.io/badge/version-v1.0.1-blue)](../../releases)
[![Platform](https://img.shields.io/badge/platform-OriginOS%206%20%2F%20Android%2016-green)]()
[![Requires](https://img.shields.io/badge/需要-root%20%2B%20LSPosed-orange)]()
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](LICENSE)

---

## 这是什么

vivo 设置「屏幕刷新率 → 使用高刷新率的应用」列表三大痛点：

- 没有全选按钮，几百个应用只能逐个点
- **新装应用会被系统自动塞进"关"**，不手动打开就一直锁 60Hz（实测连本模块自己的 APK 装上都被自动关了）
- 系统升级后状态经常神秘回退

装上本模块后，该列表不再影响任何应用。厂商对视频播放器等的兼容性封顶**保留**（该 60 的还是 60，避免画面异常），只解除"用户列表"这一层的锁。

**实测（V2419A，真实手指滑动，`dumpsys display` 采样）**：

| 应用状态 | 装模块前 | 装模块后 |
| --- | --- | --- |
| 列表"关"（夸克/快手等） | **锁死 60Hz** | **满速 120Hz** |
| 列表"开" / 不在列表 | 120Hz | 120Hz |

> ⚠️ vivo 触摸 Boost 只认硬件触摸：用 `adb input swipe` 测帧率恒为 60，
> 是假象。验证请真指滑动，或 `sendevent` 注入内核事件（协议见文末）。

## 工作原理

每应用锁帧的完整链路（实机反编译 vivo-services.jar / GameWatch.apk + dump 定位）：

```
设置页开关 → ContentProvider(signature权限, com.vivo.gamewatch)
  → prefs: user_manual_fps_set_prefs.xml (on/off/hidden 三集合)
  → SystemBridgeManager.setBundle → system_server (WindowRequestManager)
  → 固定模式下读 DisablePromotionSettings 段: 被关应用 mode=120 reqFps=60 ← 锁帧真身
```

本模块 hook `WindowRequestManager.getXmlSettings(String)`，强制返回
`mFpsXmlSettings`（"Settings:" 段）：

| 段 | 被关应用的值 | 换段后果 |
| --- | --- | --- |
| DisablePromotionSettings（原生效段） | reqFps=**60** | 被绕过 |
| FPS_XML_SETTINGS（强制切换到的段） | reqFps=**120** | 用户锁失效 |
| 同段内厂商封顶（如美团 reqFps=60） | 60 | 原样保留 ✅ |

**已证伪的假路径**（省你二次踩坑的时间）：

| 候选钩子 | 证伪原因 |
| --- | --- |
| `isNonHighRefreshRatePackage` | 调用点 `isPromotionSupported() \|\|` 短路，永不执行，集合恒空 |
| `mFrameRateOverrides` / `getFrameRateOverrideRefreshRate` | 仅动态壁纸等个别场景下发，不承载用户列表开关 |

## 安装

1. [Releases](../../releases) 下载 `VivoHfrAlwaysOn-v1.0.1.apk` 安装
2. LSPosed 管理器 → 模块 → 启用「vivo高刷全开」
   （作用域已静态声明为系统框架，自动勾选，无需手点）
3. **重启手机**（必须——钩子在 system_server，force-stop 应用无效）

回滚：LSPosed 禁用模块 → 重启，零残留（模块不写任何分区/配置文件）。

## 兼容性

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| V2419A · OriginOS 6 · Android 16 | ✅ **唯一实测** | 关锁应用 60→120 满帧 |
| 其他 OriginOS 6 机型 | ⚠️ 自测 | 同代框架类名大概率一致，欢迎反馈 |
| OriginOS 5 及更早 | ⚠️ 未验证 | 类/字段名可能不同；不匹配时静默跳过，无害 |
| 非 vivo 设备 | ❌ | 加载即跳过，不生效也不影响系统 |

要求：root（KernelSU/Magisk 均可）+ LSPosed 2.x（libxposed API 100）。

## 已知行为与 FAQ

**Q: 设置页开关还能用吗？**
A: 界面在、能拨，但不再影响实际帧率——这正是"全开"的含义。想临时关某个应用省电，v1.0 暂不支持排除名单，有需要请开 issue（技术上可在钩子内做 uid→包名过滤）。

**Q: 费电吗？**
A: 等效于你手动把列表全部点开。LTPO 闲置降帧不受影响，静止画面照样降。

**Q: 系统 OTA 后失效？**
A: 有可能。vivo 改了类名/字段名时钩子静默跳过（不卡系统），届时看 `logcat | grep VivoHfrAlwaysOn` 有无 `hook installed` 即可判断，欢迎带日志提 issue。

## 项目结构

```
├── app/src/main/java/com/bomo/vivohfr/
│   ├── MainModule.java        核心钩子（getXmlSettings 换段）
│   └── StatusActivity.java    启动器页（LSPosed Manager 识别所需）
├── app/src/main/resources/META-INF/xposed/
│   ├── java_init.list         入口类声明
│   ├── module.prop            libxposed API 100（101 在 LSPosed 2.2.0 下静默不加载）
│   └── scope.list             静态作用域: system
└── build.gradle.kts           release 签名读环境变量 HFR_KS_PASS，keystore 不入库
```

构建：`./gradlew :app:assembleRelease`（JDK21 + compileSdk 36；国内可将 wrapper 指向腾讯 Gradle 镜像）。
基于 [libxposed 官方模板](https://github.com/organization/LSPosed-module-template)。

## 免责声明

强制全应用高刷会增加功耗与发热；钩接 vivo 私有类属于非官方修改，系统 OTA 后行为不保证。
由此产生的任何体验/功耗问题与作者无关。风险自知，介意勿装。

## 致谢

- [LSPosed](https://github.com/LSPosed/LSPosed) 与 libxposed 模板
- 帧率测量方法学：`sendevent /dev/input/event6`（vivo_ts，Protocol B，坐标×10，BTN_TOUCH=0x14a）

## License

MIT © bomo
