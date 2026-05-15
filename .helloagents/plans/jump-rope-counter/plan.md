# 跳绳计数器 — 实施方案

## 技术栈
| 层级 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 相机 | CameraX |
| 姿态估计 | MediaPipe BlazePose (33 关键点) |
| 数据库 | Room |
| 设置 | DataStore Preferences |
| 异步 | Coroutines + Flow |
| 构建 | Gradle KTS |

## 架构
单 Activity + Compose Navigation，三层架构：
- **UI Layer**: Compose Screens + ViewModels
- **Domain Layer**: JumpDetector, RecordRepository
- **Data Layer**: PoseEngine (MediaPipe), Room DB, CameraManager (CameraX)

## 模块结构
```
com.tiaosheng.counter/
├── camera/CameraManager.kt       # CameraX 封装
├── pose/PoseEngine.kt             # MediaPipe BlazePose
├── counter/
│   ├── JumpDetector.kt           # 峰谷检测算法
│   ├── DetectionState.kt         # 状态数据类
│   └── KalmanFilter.kt           # 卡尔曼滤波
├── data/
│   ├── db/                       # Room 数据库
│   ├── repository/               # 数据仓库
│   └── preferences/              # DataStore
├── ui/
│   ├── theme/                    # Material 3 主题
│   ├── main/                     # 主界面 + HUD
│   ├── history/                  # 历史记录 + 图表
│   └── settings/                 # 设置页面
└── util/
    ├── CalorieCalculator.kt
    └── TtsManager.kt
```

## 实现顺序
15 个任务按依赖关系分 5 个阶段：

### 阶段 A：基础设施 (T1-T3)
T1: 项目脚手架 → T2: CameraX 集成 → T3: MediaPipe 集成
### 阶段 B：核心算法 (T4-T5)
T4: 卡尔曼滤波 → T5: 跳跃检测算法
### 阶段 C：主界面 (T6-T7)
T6: 主界面 HUD → T7: 交替跳模式
### 阶段 D：数据与辅助 (T8-T12)
T8: Room 数据库 → T9: 历史记录 → T10: 设置页 → T11: TTS → T12: 卡路里+自动暂停
### 阶段 E：收尾 (T13-T15)
T13: 性能优化 → T14: 异常处理 → T15: 集成测试

## 验证策略
每个任务完成后进行对应验证（单测 / 手动测试 / 真机验证），contract.json 定义最终验收标准。
