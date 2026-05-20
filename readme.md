# ETH / BTC 永续合约价格监控器( 响铃/震动 提醒)

一个 **轻量级 Android 实时行情监控工具**，用于监控 **ETH / BTC 永续合约标记价格**，并在满足条件时触发报警。

承诺: 不包含恶意代码. 不保证软件的可靠性. 不作为投资指导.

使用模式: 
1. 实时模式, 使用okx的websocket api, 价格更新频率高, 大陆用户需要vpn, 且设备可能发热
2. 省电模式, 使用价格中转服务器, 更新频率低, 不需要vpn, 减少发热
   因为安全原因, 代码和release中未包含http服务器, 您可以通过向 coin@corosy.com 发送任意邮件, 以获得使 用我自己部署的http服务器作为数据来源的apk.
   或者自行部署

本软件设计目标是：

* **简单**
* **稳定**
* **低干扰**
* **实时监控行情**

适合在 **后台持续运行监控行情**。

---

# 功能

## 实时行情

实时获取 **OKX WebSocket 标记价格**

支持：

* ETH 永续合约
* BTC 永续合约

显示：

* 实时价格
* Socket 状态
* 网络状态

---

## 条件报警

可以添加任意数量的价格条件。

支持：

```
ETH > 3600
ETH < 3200
BTC > 70000
BTC < 65000
```

触发条件时：

* 播放报警
* 界面闪屏提醒

---

## 网络异常检测

软件会自动检测：

* 网络断开
* WebSocket 停止推送

如果 **30 秒没有收到行情数据**：

触发报警。

---

## 静音模式

在图书馆或会议环境下可以开启：

```
🔇 静音模式
```

此时：

* 不播放声音
* 仍然可以闪屏提醒

---

## 屏幕闪烁提醒

报警时屏幕会：

```
红 / 白闪烁
```

可以手动关闭：

```
💡 闪屏开启 / 关闭
```

---

## 自动启动监控

打开 App 后：

```
自动开始行情监控
```

无需手动启动。

---

## 手动控制

支持按钮：

```
开始监控
停止监控
停止报警
静音模式
闪屏模式
```

---

# UI 特点

界面设计目标：

* 信息一目了然
* 大字体行情
* 清晰状态提示

状态使用图标：

```
Socket
Network
Monitoring
Alarm
```

例如：

```
Socket 已连接
网络正常
监控运行中
无报警
```

---

# 技术实现

主要技术：

```
Kotlin
Jetpack Compose
OkHttp WebSocket
Coroutines
Foreground Service
```

架构：

```
Activity
ViewModel
Repository
ForegroundService
WebSocket Client
```

数据流：

```
WebSocket
   ↓
Service
   ↓
AppBus
   ↓
ViewModel
   ↓
UI
```

---

# 数据来源

行情来自：

```
OKX WebSocket API
```

订阅：

```
mark-price
ETH-USDT-SWAP
BTC-USDT-SWAP
```

使用 **标记价格 (Mark Price)**。

---

# 运行方式

安装 APK 后：

```
打开 App
→ 自动开始监控
```

或：

```
点击 开始监控
```

---

# 构建

Android Studio：

```
Build → Build APK
```

或：

```
./gradlew assembleDebug
```

安装：

```
adb install app-debug.apk
```

---

# 免责声明

本软件：

* 不保证行情数据准确
* 不保证软件稳定运行
* 不提供投资建议

软件提供的信息仅供参考。

使用本软件产生的任何风险、损失或后果，均由用户自行承担。

---

# License

MIT License

---

如果你愿意，我可以再帮你补充三个 **GitHub 项目非常重要的部分**：

1️⃣ **README 首页截图区**（看起来更专业）
2️⃣ **功能截图展示**
3️⃣ **项目 Logo / Badge**

这样你的仓库会 **看起来像一个成熟开源项目**。
