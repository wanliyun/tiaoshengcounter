# 跳绳计数器 — 决策日志

决策记录贯穿 PRD 全程，记录每个维度讨论中的关键决策。

## 决策列表
- [2026-05-15] D-001: 姿态估计引擎选型
  - 背景：需要选型一个 Android 端离线姿态估计引擎，支持脚踝关键点检测，性能满足实时要求
  - 选项：A) MediaPipe BlazePose B) Google ML Kit Pose Detection C) 阿里 MNN + 转换 OpenPose/MoveNet 模型 D) YOLOv8-Pose + NCNN
  - 结论：选项 A — MediaPipe BlazePose
  - 理由：MediaPipe 提供 33 个关键点(含脚踝)，而 ML Kit 仅 17 个(无脚踝)；MediaPipe Android SDK 成熟且文档丰富；GPU/NNAPI 代理原生支持；模型已优化为 TFLite 无需额外转换。MNN 方案虽轻量但需要自己完成模型转换和 Android 集成胶水代码。BlazePose 在姿态估计领域有广泛验证的使用案例。

- [2026-05-15] D-002: 跳跃检测锚点关键点选择
  - 背景：需要确定使用哪个/哪些身体关键点的运动轨迹来判断跳跃事件
  - 选项：A) 脚踝(ankle) Y 坐标 B) 骨盆(pelvis) Y 坐标 C) 脚踝+骨盆双信号融合
  - 结论：选项 A — 脚踝关键点（双脚跳取均值，交替跳独立追踪）
  - 理由：参考 YOLOv8-Pose 跳绳论文，脚踝 Y 轴震荡幅度明显大于骨盆(脚踝幅值约为骨盆 2-3 倍)，信噪比更高，算法更鲁棒；双脚跳取均值抵消左右偏摆噪声；交替跳分别追踪保证不丢次。

- [2026-05-15] D-003: UI 框架选型
  - 背景：Android UI 有 XML View 和 Jetpack Compose 两种主要方案
  - 选项：A) Jetpack Compose B) XML View + DataBinding
  - 结论：选项 A — Jetpack Compose
  - 理由：Compose 已是 Android 官方推荐方案；CameraX 与 Compose 集成成熟；声明式 UI 更易于实现 HUD 覆盖层状态切换和动画；无需维护 XML+ViewBinding 样板代码。

- [2026-05-15] D-004: 计数算法核心策略
  - 背景：需确定跳跃检测的核心信号处理策略
  - 选项：A) 滑动窗口峰谷检测 + 卡尔曼滤波 B) 简单阈值法(脚踝 Y < 固定值即跳跃) C) 机器学习分类器(训练跳跃/非跳跃分类器)
  - 结论：选项 A — 滑动窗口峰谷检测 + 卡尔曼滤波
  - 理由：阈值法对不同身高/距离的用户需要频繁调整，鲁棒性差；ML 分类器方案工程复杂度高且缺乏标注训练数据；峰谷检测算法成熟(论文验证准确率 95-98%)，配合自适应阈值(基于滑动窗口统计)对不同用户自动适应，工程复杂度适中。

- [2026-05-15] D-005: 模型部署策略
  - 背景：需要确定姿态估计模型如何打包到 APK
  - 选项：A) 内嵌 APK assets B) 首次启动下载 C) 按需动态分发
  - 结论：选项 A — 内嵌 APK assets
  - 理由：核心约束是完全离线，不依赖网络；BlazePose Lite 模型约 5MB，对 APK 体积影响可控(~25MB 代码+资源+5MB 模型=~30MB)；避免了下载失败、网络错误等异常路径。

- [2026-05-15] D-006: 本地存储方案
  - 背景：需要选型运动记录的本地持久化方案
  - 选项：A) Room (SQLite) B) DataStore C) 文件直接序列化 (JSON)
  - 结论：选项 A — Room
  - 理由：运动记录有结构化查询需求(按日/周/月聚合、排序、分页)；Room 提供编译期 SQL 校验、Flow 响应式查询、迁移支持；DataStore 适合键值设置但不适合列表/聚合查询；JSON 文件在大数据量下性能和可靠性差。

- [2026-05-15] D-007: 不声明 INTERNET 权限
  - 背景：是否在 AndroidManifest 中声明 INTERNET 权限
  - 选项：A) 不声明 B) 声明但不使用
  - 结论：选项 A — 不声明 INTERNET 权限
  - 理由：应用定位完全离线，声明未使用权限会降低用户信任；Google Play 审核中 "No data collection" + 无网络权限是最强隐私信号；如后续版本需要(崩溃上报/应用更新检查)，届时再加。
