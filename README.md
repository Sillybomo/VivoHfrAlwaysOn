# vivo高刷全开 (LSPosed 模块) v1.0

让 vivo/OriginOS "使用高刷新率的应用" 列表的用户开关彻底失效——所有应用（含新装、含被 vivo 自动置关的）按高刷执行。

## 真实机制（V2419A / OriginOS16 实机反编译 + dump + 行为测试定位）

1. 每应用开关存储在 `com.vivo.gamewatch` 的
   `/data/user_de/0/com.vivo.gamewatch/shared_prefs/user_manual_fps_set_prefs.xml`
   （off 集合 = 被关闭的应用；不在集合里的应用默认开）
2. 装机/更新应用时 vivo 会把部分应用**自动写入 off 集合**（实测本模块自己装上就被自动关了）——这就是"新应用要手动开"的根源
3. 框架侧真实执行点：固定模式（"高刷新率固定"开、`isPromotionEnabled:false`）下
   `com.vivo.services.rrm.WindowRequestManager.getXmlSettings(sEffectedSetting)` 返回
   `DisablePromotionSettings` 段，被关应用在该段 `reqFps=60` → 真实触摸也只跑 60Hz
4. 本模块 hook `getXmlSettings`，强制返回 `mFpsXmlSettings`（"Settings:" 段）：
   - 用户开关的 60 锁被绕过（该段里被关应用仍是 reqFps=120）
   - 厂商基础兼容封顶保留（如美团在该段本身 reqFps=60，不受影响）
   - 新应用天然无锁

## 已排除的假路径（勿再尝试）

- `isNonHighRefreshRatePackage`：promotion 硬件支持时调用点 `||` 短路，永不执行，集合恒空
- `mFrameRateOverrides`/`getFrameRateOverrideRefreshRate`：仅个别场景下发（动态壁纸），不覆盖用户列表开关

## 安装

1. 装 APK → LSPosed 管理器启用（作用域静态声明=系统框架）→ **重启**（system_server 钩子必须重启）
2. 卸载即回滚，零残留

## 验证方法与结论

⚠️ vivo 触摸 Boost 只认硬件触摸：`adb input swipe` 注入事件不触发升帧（恒 60），
必须用 `sendevent /dev/input/event6`(vivo_ts, Protocol B, 坐标×10, BTN_TOUCH=0x14a) 模拟真实触摸。

实测：夸克在 off 集合中，装模块前真实触摸 60Hz，装模块后真实触摸 120Hz。

## 构建

```sh
./gradlew :app:assembleRelease   # keystore/vivohfr.jks (口令 vivohfr)
```

基于 libxposed 官方模板 organization/LSPosed-module-template（minApiVersion 改 100 以兼容 LSPosed 2.2.0）。

@author bomo
