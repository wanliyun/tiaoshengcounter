# 跳绳计数器 — 技术架构

## 技术选型与理由
| 层级 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | Android 官方首选语言，协程支持 |
| UI 框架 | Jetpack Compose | 声明式 UI，Material 3，CameraX 集成良好 |
| 相机 | CameraX | Jetpack 官方库，生命周期感知，ImageAnalysis 支持 |
| 姿态估计 | MediaPipe BlazePose | 33 关键点(含脚踝)，Android 原生支持，TFLite 模型，GPU/NNAPI 加速，完全离线 |
| 本地数据库 | Room | Jetpack 官方 ORM，编译期 SQL 校验，Flow 响应式查询 |
| 异步 | Kotlin Coroutines + Flow | 标准异步方案，Room/CameraX 原生支持 |
| 构建 | Gradle KTS + Version Catalog | 现代化构建配置，依赖集中管理 |
| 最低 SDK | API 26 (Android 8.0) | 覆盖 95%+ 活跃设备，CameraX 最低要求 |

## 系统架构图
```
┌─────────────────────────────────────────────────┐
│                    UI Layer (Compose)              │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ MainScreen│  │HistoryScreen│ │SettingsScreen│  │
│  └────┬─────┘  └─────┬─────┘  └──────┬────────┘  │
│       │              │               │           │
│  ┌────┴──────────────┴───────────────┴────────┐  │
│  │              ViewModel Layer                │  │
│  │  ┌───────────┐  ┌────────────────────────┐ │  │
│  │  │MainViewModel│  │HistoryViewModel       │ │  │
│  │  │ - counter  │  │ - records: Flow<List>  │ │  │
│  │  │ - state    │  │ - summaries: Flow<>    │ │  │
│  │  └─────┬─────┘  └───────────┬────────────┘ │  │
│  └────────┼────────────────────┼──────────────┘  │
├───────────┼────────────────────┼──────────────────┤
│           │          Domain Layer                │
│  ┌────────┴────────┐  ┌───────┴──────────┐      │
│  │  JumpDetector   │  │  RecordRepository │      │
│  │  - detect()     │  │  - save/get/query │      │
│  │  - mode switch  │  │  - exportCsv()    │      │
│  └────────┬────────┘  └───────┬──────────┘      │
├───────────┼────────────────────┼──────────────────┤
│           │           Data Layer                │
│  ┌────────┴────────┐  ┌───────┴──────────┐      │
│  │  PoseEngine     │  │  Room Database    │      │
│  │  - processFrame │  │  - ExerciseDao   │      │
│  │  - landmarks    │  │  - Entities       │      │
│  └────────┬────────┘  └──────────────────┘      │
│           │                                      │
│  ┌────────┴────────┐                             │
│  │  CameraManager  │                             │
│  │  (CameraX)      │                             │
│  └─────────────────┘                             │
└─────────────────────────────────────────────────┘
```

## 文件/模块结构
```
app/
├── src/main/java/com/tiaosheng/counter/
│   ├── TiaoshengApp.kt              # Application
│   ├── MainActivity.kt               # 单 Activity 入口
│   │
│   ├── camera/
│   │   └── CameraManager.kt          # CameraX 封装：预览 + ImageAnalysis
│   │
│   ├── pose/
│   │   └── PoseEngine.kt             # MediaPipe BlazePose 封装
│   │
│   ├── counter/
│   │   ├── JumpDetector.kt           # 跳跃检测核心算法
│   │   ├── DetectionState.kt         # 检测状态数据类
│   │   └── KalmanFilter.kt           # 一维卡尔曼滤波器（关键点平滑）
│   │
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt        # Room 数据库
│   │   │   ├── ExerciseDao.kt        # DAO
│   │   │   └── ExerciseEntity.kt     # 实体
│   │   ├── repository/
│   │   │   └── RecordRepository.kt   # 数据仓库
│   │   └── preferences/
│   │       └── SettingsStore.kt       # DataStore/SharedPreferences
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Type.kt
│   │   ├── main/
│   │   │   ├── MainScreen.kt         # 主界面：摄像头 + HUD
│   │   │   ├── MainViewModel.kt
│   │   │   └── HudOverlay.kt         # 计数/计时/BPM 覆盖层
│   │   ├── history/
│   │   │   ├── HistoryScreen.kt
│   │   │   ├── HistoryViewModel.kt
│   │   │   └── StatsChart.kt         # 简易柱状图组件
│   │   └── settings/
│   │       ├── SettingsScreen.kt
│   │       └── SettingsViewModel.kt
│   │
│   └── util/
│       ├── CalorieCalculator.kt      # 卡路里计算工具
│       └── TtsManager.kt             # TTS 播报管理
│
├── src/main/assets/
│   └── pose_landmarker_lite.task     # MediaPipe 模型文件
│
└── build.gradle.kts
```

## 依赖关系与第三方服务
```kotlin
// 核心依赖
implementation("com.google.mediapipe:tasks-vision:0.10.14")   // BlazePose
implementation("androidx.camera:camera-camera2:1.3.4")        // CameraX
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")
implementation("androidx.room:room-runtime:2.6.1")            // Room
implementation("androidx.room:room-ktx:2.6.1")
implementation("androidx.compose.material3:material3")         // Material 3
implementation("androidx.datastore:datastore-preferences:1.1.1")
// 无远程网络库依赖
```

## 计数算法详细设计

### 输入
- MediaPipe BlazePose 输出的 33 个 3D 关键点 (x, y, z, visibility)
- 关键点索引: 左踝 27, 右踝 28
- 帧率: ~30 FPS（取决于设备性能）

### 数据流
```
原始帧 (CameraX ImageProxy, YUV420)
  → Bitmap 转换 (缩放到 320x240)
    → MediaPipe PoseLandmarker.detect(bitmap)
      → PoseLandmarkerResult (每帧 33 个关键点)
        → 提取 ankle[27], ankle[28].y (归一化 0~1)
          → KalmanFilter 平滑 (降噪)
            → JumpDetector 峰谷检测
              → 输出: jumpCount, bpm, jumpState
```

### 峰谷检测算法
```
状态: TRACKING (寻找波峰) / VALLEY (寻找波谷)

1. 维护长度为 N=90 (约3秒) 的滑动窗口, 存储平滑后的脚踝 Y 值
2. 每新帧:
   a. 卡尔曼滤波平滑: y_filtered = kalmanFilter.update(raw_y)
   b. 追加到滑动窗口
   c. 计算最近窗口的均值和标准差
   d. 在 TRACKING 状态:
      - 若当前值低于 (均值 - sensitivity_threshold * 标准差): 进入 VALLEY
      - sensitivity_threshold: 低=1.5, 中=1.2, 高=0.9
   e. 在 VALLEY 状态:
      - 若当前值高于 (均值 + 0.5 * 标准差): 确认一次跳跃
      - 检查距上次计数 ≥150ms (约5帧@30fps): count++, 切换到 TRACKING
      - 否则: 忽略(去毛刺)

双脚跳: ankle_y = (left_ankle.y + right_ankle.y) / 2
交替跳: 左右脚踝独立追踪, 任一满足即计为 1 次
```

### 卡尔曼滤波器（一维）
```
状态: [position, velocity]
测量: position (ankle_y)
过程噪声 Q: 0.01 (假设跳绳为周期性匀速运动)
测量噪声 R: 0.1 (摄像头检测有一定噪声)
```

## 性能优化策略
- 帧处理分辨率 320x240（足够关键点检测，大幅减少推理延迟）
- ImageAnalysis 背压策略: STRATEGY_KEEP_ONLY_LATEST（丢弃积压帧）
- MediaPipe 代理: GPU > NNAPI > CPU，运行时自动选择
- Compose UI 使用 derivedStateOf 避免不必要的重组
- 腔数据在 IO 协程调度器上操作
